package org.tribbloid.spookystuff

import org.tribbloid.spookystuff.actions._
import org.tribbloid.spookystuff.entity.PageRow
import org.tribbloid.spookystuff.pages.Page

/**
 * Created by peng on 12/2/14.
 */
package object expressions {

  type Expression[+R] = NamedFunction1[PageRow, Option[R]]

  type PageFilePath[+R] = (Page => R)

  type CacheFilePath[+R] = (Trace => R)
}