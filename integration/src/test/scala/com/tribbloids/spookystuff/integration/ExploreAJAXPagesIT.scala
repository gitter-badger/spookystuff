package com.tribbloids.spookystuff.integration

import com.tribbloids.spookystuff.SpookyContext
import com.tribbloids.spookystuff.actions._
import com.tribbloids.spookystuff.dsl._

import scala.concurrent.duration._

/**
 * Created by peng on 12/10/14.
 */
class ExploreAJAXPagesIT extends IntegrationSuite {

  override lazy val drivers = Seq(
    DriverFactories.PhantomJS() //TODO: HtmlUnit does not support Backbone.js
  )

  override def doMain(spooky: SpookyContext): Unit = {

    val snapshotAllPages = (
      Snapshot() ~ 'first
        +> Loop (
        ClickNext("button.btn", "1"::Nil)
          +> Delay(2.seconds)
          +> Snapshot()
      ))

    val base = spooky
      .fetch(
        Visit("http://webscraper.io/test-sites/e-commerce/ajax")
          +> snapshotAllPages
      )

    val result = base
      .explore(S"div.sidebar-nav a", depthKey = 'depth, ordinalKey = 'index)(
        Visit('A.href)
          +> snapshotAllPages,
        flattenPagesOrdinalKey = 'page_index
      )(
        S"button.btn.btn-primary".text ~ 'page_number,
        'A.text ~ 'category,
        'first.findAll("h1").text ~ 'title,
        S"a.title".size ~ 'num_product
      )
      .toDF(sort = true)

    assert(
      result.schema.fieldNames ===
        "depth" ::
          "index" ::
          "page_index" ::
          "page_number" ::
          "category" ::
          "title" ::
          "num_product" :: Nil
    )

    val formatted = result.toJSON.collect().mkString("\n")
    assert(
      formatted ===
        """
          |{"depth":0,"title":"E-commerce training site","num_product":3}
          |{"depth":1,"index":[1],"page_index":[0],"category":"Computers","title":"Computers category","num_product":3}
          |{"depth":1,"index":[2],"page_index":[0],"category":"Phones","title":"Phones category","num_product":3}
          |{"depth":2,"index":[1,2],"page_index":[0,0],"page_number":"1","category":"Laptops","title":"Computers / Laptops","num_product":6}
          |{"depth":2,"index":[1,2],"page_index":[0,1],"page_number":"2","category":"Laptops","num_product":6}
          |{"depth":2,"index":[1,2],"page_index":[0,2],"page_number":"3","category":"Laptops","num_product":6}
          |{"depth":2,"index":[1,3],"page_index":[0,0],"page_number":"1","category":"Tablets","title":"Computers / Tablets","num_product":6}
          |{"depth":2,"index":[1,3],"page_index":[0,1],"page_number":"2","category":"Tablets","num_product":6}
          |{"depth":2,"index":[1,3],"page_index":[0,2],"page_number":"3","category":"Tablets","num_product":6}
          |{"depth":2,"index":[1,3],"page_index":[0,3],"page_number":"4","category":"Tablets","num_product":6}
          |{"depth":2,"index":[2,3],"page_index":[0,0],"page_number":"1","category":"Touch","title":"Phones / Touch","num_product":6}
          |{"depth":2,"index":[2,3],"page_index":[0,1],"page_number":"2","category":"Touch","num_product":6}
        """.stripMargin.trim
    )
  }

  override def numSessions =  6

  override def numPages = _ => 12
}
