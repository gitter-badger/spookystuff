package com.tribbloids.spookystuff.actions

import com.tribbloids.spookystuff.dsl.DriverFactories

/**
 * Created by peng on 2/19/15.
 */
class TestInteractionWithHtmlUnit extends TestInteraction {

  override lazy val driverFactory = DriverFactories.HtmlUnit()

  //TODO: current phantomjs is buggy and cannot handle these two, which is why they are here
  //TODO: current HtmlUnit is buggy and cannot handle these two
//  test("click should not double click") {
//    spooky.conf.remoteResourceTimeout = 180.seconds
//
//    try {
//      val results = (Visit("https://ca.vwr.com/store/search?&pimId=582903")
//        +> Paginate("a[title=Next]", delay = 2.second)).head.self.resolve(spooky)
//
//      val numPages = results.head.asInstanceOf[Page].findAll("div.right a").size
//
//      assert(results.size == numPages)
//    }
//
//    finally {
//      spooky.conf.remoteResourceTimeout = 60.seconds
//    }
//  }

//  test("dynamic paginate should returns right number of pages") {
//    spooky.conf.remoteResourceTimeout = 180.seconds
//
//    try {
//      val results = (Visit("https://ca.vwr.com/store/search?label=Blotting%20Kits&pimId=3617065")
//        +> Paginate("a[title=Next]", delay = 2.second)).head.self.resolve(spooky)
//
//      val numPages = results.head.asInstanceOf[Page].findAll("div.right a").size
//
//      assert(results.size == numPages)
//    }
//
//    finally {
//      spooky.conf.remoteResourceTimeout = 60.seconds
//    }
//  }
}