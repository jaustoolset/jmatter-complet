package com.u2d.type.atom;

import com.u2d.model.*;
import com.u2d.find.inequalities.NumericalInequalities;
import com.u2d.find.Searchable;
import java.text.ParseException;
import java.util.List;

/**
 * A latitude or longitude
 */
public class GeoValue extends AbstractAtomicEO implements Searchable
{
   public static final double RADIANS_PER_DEGREE = Math.PI / 180;
   public static final double SECONDS_PER_DEGREE = 3600;

   private double _seconds;  // 1/3600 of a degree
   // direction encoded as either a positive or negative value
   // where W or N is positive
   
   public GeoValue() {}
   
   public GeoValue(double value)
   {
      _seconds = value;
   }
   
   public float floatValue() { return (float) _seconds; }
   public double doubleValue() { return _seconds; }
   public double radianValue()
   {
      return _seconds / SECONDS_PER_DEGREE * RADIANS_PER_DEGREE;
   }

   public double degreesValue()
   {
      return _seconds / 3600;
   }
   public static double degreesToSeconds(double degrees)
   {
      return degrees * SECONDS_PER_DEGREE;
   }

   public void setValue(double value)
   {
      _seconds = value;
      fireStateChanged();
   }
   public void setValue(EObject value)
   {
      if (!(value instanceof GeoValue))
         throw new IllegalArgumentException("Invalid type on set;  must be Percent");
      setValue(((GeoValue) value).doubleValue());
   }
   
   public boolean isEmpty() { return false; }
   
   public Title title()
   {
      return new Title(formattedValue());
   }

   private String formattedValue()
   {
      return String.valueOf(degreesValue());
   }

   public String toString()
   {
      return title().toString();
   }
   
   public boolean equals(Object obj)
   {
      if (obj == null) return false;
      if (obj == this) return true;
      if (!(obj instanceof GeoValue)) return false;
      return _seconds == ((GeoValue) obj).doubleValue();
   }

   public int hashCode() { return new Double(_seconds).hashCode(); }

   public AtomicRenderer getRenderer() { return vmech().getDegreeRenderer(); }
   public AtomicEditor getEditor() { return vmech().getDegreeEditor(); }

   // assume value is entered/specified in degrees..
   public void parseValue(String stringValue) throws ParseException
   {
      double degrees = Double.parseDouble(stringValue);
      setValue(degreesToSeconds(degrees));
   }

   public EObject makeCopy()
   {
      return new GeoValue(this.doubleValue());
   }

   // ===
   
   public List getInequalities()
   {
      return new NumericalInequalities(field()).getInequalities();
   }
}
