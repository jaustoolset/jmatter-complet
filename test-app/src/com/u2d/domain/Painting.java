package com.u2d.domain;

import com.u2d.model.AbstractComplexEObject;
import com.u2d.model.Title;
import com.u2d.type.atom.StringEO;
import javax.persistence.Entity;

@Entity
public class Painting extends AbstractComplexEObject
{
   private final StringEO name = new StringEO();

   public Painting()
   {
   }

   public StringEO getName()
   {
      return name;
   }

   public Title title()
   {
      return name.title();
   }
}
