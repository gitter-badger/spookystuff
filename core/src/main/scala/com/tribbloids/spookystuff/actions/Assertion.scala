package com.tribbloids.spookystuff.actions

import com.tribbloids.spookystuff.pages.Page
import com.tribbloids.spookystuff.session.Session

/**
 * Created by peng on 1/21/15.
 */
abstract class Assertion extends Action {

  final override def outputNames = Set()

  final override def trunk = None //can't be omitted

  final override def doExe(session: Session): Seq[Page] = {

    exeWithoutPage(session: Session)

    Nil
  }

  def exeWithoutPage(session: Session): Unit
}

case class Assert(condition: Page => Boolean) extends Assertion {
  override def exeWithoutPage(session: Session): Unit = {
    val page = DefaultSnapshot.apply(session).head.asInstanceOf[Page]

    assert(condition(page))
  }
}