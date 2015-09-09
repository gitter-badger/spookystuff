package org.tribbloid.spookystuff.actions

import java.util.Date

import org.tribbloid.spookystuff.SpookyEnvSuite
import org.tribbloid.spookystuff.expressions.Literal

/**
 * Created by peng on 08/09/15.
 */
class TestBlock extends SpookyEnvSuite {

  import org.tribbloid.spookystuff.dsl._

  import scala.concurrent.duration._

  test("loop without export won't need driver") {

    val loop = Loop(
      Delay(10.seconds) +> Wget("ftp://www.dummy.co")
    )

    assert(!loop.needDriver)
  }

  test("try without export won't need driver") {
    import scala.concurrent.duration._

    val tryy = Try(
      Delay(10.seconds) +> Wget("ftp://www.dummy.org")
    )

    assert(!tryy.needDriver)
  }

  test("wayback time of loop should be identical to its last child supporting wayback") {
    val waybackDate = new Date()

    val loop = Loop(
      Delay(10.seconds) +> Wget("ftp://www.dummy.co").waybackTo(waybackDate)
    +> Delay(20.seconds) +> Wget("ftp://www.dummy2.co").waybackToTimeMillis(waybackDate.getTime - 100000)
    )

    assert(loop.wayback == Literal[Long](waybackDate.getTime - 100000))
  }
}
