package com.tribbloids.spookystuff

import com.tribbloids.spookystuff.rdd.FetchedDataset
import com.tribbloids.spookystuff.row._
import com.tribbloids.spookystuff.utils.HDFSResolver
import org.apache.hadoop.conf.Configuration
import org.apache.spark._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.dsl.utils.MessageWrapper
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.storage.StorageLevel

import scala.collection.immutable.ListMap
import scala.language.implicitConversions
import scala.reflect.ClassTag

case class SpookyContext private (
                                   @transient sqlContext: SQLContext, //can't be used on executors
                                   @transient private var _conf: SpookyConf, //can only be used on executors after broadcast
                                   metrics: Metrics //accumulators cannot be broadcasted,
                                 ) {

  def this(
            sqlContext: SQLContext,
            conf: SpookyConf = new SpookyConf()
          ) {
    this(sqlContext, conf.importFrom(sqlContext.sparkContext), new Metrics())
  }

  def this(sqlContext: SQLContext) {
    this(sqlContext, new SpookyConf())
  }

  def this(sc: SparkContext) {
    this(new SQLContext(sc))
  }

  def this(conf: SparkConf) {
    this(new SparkContext(conf))
  }

  import com.tribbloids.spookystuff.utils.ImplicitUtils._
  import org.apache.spark.sql.catalyst.ScalaReflection.universe._

  def isOnDriver: Boolean = sqlContext != null
  if (isOnDriver) {
    conf.webDriverFactory.deploy(this)
    conf.pythonDriverFactory.deploy(this)
  }

  def sparkContext = this.sqlContext.sparkContext

  @volatile var broadcastedEffectiveConf = sqlContext.sparkContext.broadcast(_conf)

  def conf = if (_conf == null) broadcastedEffectiveConf.value
  else _conf

  def conf_=(conf: SpookyConf): Unit = {
    _conf = conf.importFrom(sqlContext.sparkContext)
    rebroadcast()
  }

  def rebroadcast(): Unit ={
    try {
      broadcastedEffectiveConf.destroy()
    }
    catch {
      case e: Throwable =>
    }
    broadcastedEffectiveConf = sqlContext.sparkContext.broadcast(_conf)
  }

  val broadcastedHadoopConf: Broadcast[SerializableWritable[Configuration]] =
    if (sqlContext!=null) {
      sqlContext.sparkContext.broadcast(
        new SerializableWritable(this.sqlContext.sparkContext.hadoopConfiguration)
      )
    }
    else null

  @transient lazy val hadoopConf: Configuration = broadcastedHadoopConf.value.value
  @transient lazy val resolver = HDFSResolver(hadoopConf)

  def zeroMetrics(): SpookyContext ={
    metrics.clear()
    this
  }

  def getSpookyForRDD = {
    if (conf.shareMetrics) this
    else this.copy(metrics = new Metrics())
  }

  def create(df: DataFrame): FetchedDataset = this.dsl.dataFrameToPageRowRDD(df)
  def create[T: TypeTag](rdd: RDD[T]): FetchedDataset = this.dsl.rddToPageRowRDD(rdd)

  def create[T: TypeTag](
                          seq: TraversableOnce[T]
                        ): FetchedDataset = {

    implicit val ctg = implicitly[TypeTag[T]].classTag
    this.dsl.rddToPageRowRDD(this.sqlContext.sparkContext.parallelize(seq.toSeq))
  }

  def create[T: TypeTag](
                          seq: TraversableOnce[T],
                          numSlices: Int
                        ): FetchedDataset = {

    implicit val ctg = implicitly[TypeTag[T]].classTag
    this.dsl.rddToPageRowRDD(this.sqlContext.sparkContext.parallelize(seq.toSeq, numSlices))
  }

  lazy val blankSelfRDD = sparkContext.parallelize(Seq(SquashedFetchedRow.blank))

  def createBlank = this.create(blankSelfRDD)

  def createBeaconRDD[K: ClassTag,V: ClassTag](
                                                ref: RDD[_],
                                                partitionerFactory: RDD[_] => Partitioner = conf.defaultPartitionerFactory
                                              ): RDD[(K,V)] = {
    sparkContext
      .emptyRDD[(K,V)]
      .partitionBy(partitionerFactory(ref))
      .persist(StorageLevel.MEMORY_ONLY)
  }

  object dsl extends Serializable {

    import com.tribbloids.spookystuff.utils.ImplicitUtils._

    implicit def dataFrameToPageRowRDD(df: DataFrame): FetchedDataset = {
      val self: SquashedFetchedRDD = new DataFrameView(df)
        .toMapRDD(false)
        .map {
          map =>
            SquashedFetchedRow(
              Option(ListMap(map.toSeq: _*))
                .getOrElse(ListMap())
                .map(tuple => (Field(tuple._1), tuple._2))
            )
        }
      val fields = df.schema.fields.map {
        sf =>
          Field(sf.name) -> sf.dataType
      }
      new FetchedDataset(
        self,
        fieldMap = ListMap(fields: _*),
        spooky = getSpookyForRDD
      )
    }

    //every input or noInput will generate a new metrics
    implicit def rddToPageRowRDD[T: TypeTag](rdd: RDD[T]): FetchedDataset = {

      val ttg = implicitly[TypeTag[T]]

      rdd match {
        // RDD[Map] => JSON => DF => ..
        case _ if ttg.tpe <:< typeOf[Map[_,_]] =>
          //        classOf[Map[_,_]].isAssignableFrom(classTag[T].runtimeClass) => //use classOf everywhere?
          val canonRdd = rdd.map(
            map =>map.asInstanceOf[Map[_,_]].canonizeKeysToColumnNames
          )

          val jsonRDD = canonRdd.map(
            map =>
              MessageWrapper(map).compactJSON()
          )
          val dataFrame = sqlContext.read.json(jsonRDD)
          dataFrameToPageRowRDD(dataFrame)

        // RDD[SquashedFetchedRow] => ..
        //discard schema
        case _ if ttg.tpe <:< typeOf[SquashedFetchedRow] =>
          //        case _ if classOf[SquashedFetchedRow] == classTag[T].runtimeClass =>
          val self = rdd.asInstanceOf[SquashedFetchedRDD]
          new FetchedDataset(
            self,
            fieldMap = ListMap(),
            spooky = getSpookyForRDD
          )

        // RDD[T] => RDD('_ -> T) => ...
        case _ =>
          val self = rdd.map{
            str =>
              var cells = ListMap[Field,Any]()
              if (str!=null) cells = cells + (Field("_") -> str)

              SquashedFetchedRow(cells)
          }
          new FetchedDataset(
            self,
            fieldMap = ListMap(Field("_") -> ttg.catalystType),
            spooky = getSpookyForRDD
          )
      }
    }
  }
}