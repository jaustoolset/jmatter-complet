package com.u2d.sympster;

import com.u2d.model.ComplexEObject;
import com.u2d.persist.Persist;
import com.u2d.type.atom.StringEO;

/**
 * Created by IntelliJ IDEA.
 * User: eitan
 * Date: Sep 6, 2006
 * Time: 5:06:00 PM
 */
@Persist
public interface Event extends ComplexEObject
{
   public StringEO getTitle();
}
