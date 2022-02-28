package de.tostsoft.solarmonitoring.utils;

public class NumberComparator {
  public static boolean compare(Double f1,Double f2){
    if(f1 == null && f2 == null){
      return true;
    }
    if(f1 != null && f2 != null){
      return Double.compare(f1,f2) == 0;
    }
    return false;
  }

  public static boolean compare(Integer f1,Integer f2){
    if(f1 == null && f2 == null){
      return true;
    }
    if(f1 != null && f2 != null){
      return Integer.compare(f1,f2) == 0;
    }
    return false;
  }
}
