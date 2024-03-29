package demo

import java.io._

class Point(val xc: Int, val yc: Int) {
   var x: Int = xc
   var y: Int = yc
   
   def move(dx: Int, dy: Int) {
      x = x + dx
      y = y + dy
      println ("Point x location : " + x);
      println ("Point y location : " + y);
   }
}

object Demo {
   def main(args: Array[String]) {
      val myVal0 :String = "program ";
      val myVal1 = "start.";
      
      println(myVal0); println(myVal1);
      
      val pt = new Point(10, 20);

      // Move to a new location
      pt.move(10, 10);
   }
}