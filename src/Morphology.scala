
import org.nlogo.api._
import org.nlogo.api.Syntax._
import org.nlogo.api.ScalaConversions._
import org.apache.commons.math3.stat.regression.SimpleRegression
import org.apache.commons.math3.util.MathArrays
import scala.math._


class Morphology extends DefaultClassManager {
  def load(manager: PrimitiveManager) {
    manager.addPrimitive("get-patch-var", new GetPatchVar)
    manager.addPrimitive("slope", new Slope)
    manager.addPrimitive("distance", new Distance)
    manager.addPrimitive("entropy", new Entropy)
    manager.addPrimitive("moran", new Moran)
  }
}



class GetPatchVar extends DefaultReporter {
  override def getSyntax = reporterSyntax(Array(NumberType), ListType)

  def report(args: Array[Argument], context: Context): AnyRef = {
    val n = args(0).getIntValue + 5 // add constant value to skip built-in patch variables
    def world = context.getAgent.world;
    //def patchVar = (_reference)((org.nlogo.nvm.Argument) args(0)).getReporter();
    //def patchVar = ((ReferenceType) args(0)).getReporter
    //Dimension gridSize = new Dimension(world.worldWidth(), world.worldHeight());

    var res = Seq.empty[org.nlogo.api.LogoList]

    for (px <- world.minPxcor to world.maxPxcor) {
      var currentrow = Seq.empty[Double]
      for (py <- world.minPycor to world.maxPycor) {
          def p = world.fastGetPatchAt(px, py);
            //println(p.getVariable(patchVar.reference.vn()));
            currentrow = currentrow :+ p.getVariable(n).asInstanceOf[Double]
          }
          res = res :+ currentrow.toLogoList
      }

      return(res.toLogoList)

  }
}


class Slope extends DefaultReporter {
  override def getSyntax = reporterSyntax(Array(NumberType), ListType)
  def report(args: Array[Argument], context: Context): AnyRef = {
     def patchVar =  Tools.getPatchVar(args,context)
     return(List(Measures.slope(patchVar)).toLogoList)
  }
}

class Distance extends DefaultReporter {
  override def getSyntax = reporterSyntax(Array(NumberType), NumberType)
  def report(args: Array[Argument], context: Context): AnyRef = {
     def patchVar =  Tools.getPatchVar(args,context)
     val res: java.lang.Double = Measures.distance_convol(patchVar)
     return(res)
  }
}

class Entropy extends DefaultReporter {
  override def getSyntax = reporterSyntax(Array(NumberType), NumberType)
  def report(args: Array[Argument], context: Context): AnyRef = {
     def patchVar =  Tools.getPatchVar(args,context)
     val res: java.lang.Double = Measures.entropy(patchVar)
     return(res)
  }
}


class Moran extends DefaultReporter {
  override def getSyntax = reporterSyntax(Array(NumberType), NumberType)
  def report(args: Array[Argument], context: Context): AnyRef = {
     def patchVar =  Tools.getPatchVar(args,context)
     val res: java.lang.Double = Measures.moran_convol(patchVar)
     return(res)
  }
}


object Tools {


   //get given patch var as Seq[Seq[Double]]
   def getPatchVar(args: Array[Argument], context: Context): Seq[Seq[Double]] = {
     val n = args(0).getIntValue + 5 // add constant value to skip built-in patch variables
     def world = context.getAgent.world;
     //def patchVar = (_reference)((org.nlogo.nvm.Argument) args(0)).getReporter();
     //def patchVar = ((ReferenceType) args(0)).getReporter
     //Dimension gridSize = new Dimension(world.worldWidth(), world.worldHeight());

     var res = Seq.empty[Seq[Double]]

     for (px <- world.minPxcor to world.maxPxcor) {
       var currentrow = Seq.empty[Double]
       for (py <- world.minPycor to world.maxPycor) {
           def p = world.fastGetPatchAt(px, py);
             //println(p.getVariable(patchVar.reference.vn()));
             currentrow = currentrow :+ p.getVariable(n).asInstanceOf[Double]
           }
           res = res :+ currentrow
       }

       return(res)
   }

}




object Measures {

  def slope(matrix: Seq[Seq[Double]]) = {
    def distribution = matrix.flatten.sorted(Ordering.Double.reverse).filter(_ > 0)
    def distributionLog = distribution.zipWithIndex.map { case (q, i) => Array(log(i + 1), log(q)) }.toArray
    val simpleRegression = new SimpleRegression(true)
    simpleRegression.addData(distributionLog)
    (simpleRegression.getSlope(), simpleRegression.getRSquare())
  }



    def distanceMean(matrix: Seq[Seq[Double]]) = {

      def totalQuantity = matrix.flatten.sum

      def numerator =
        (for {
          (c1, p1) <- zipWithPosition(matrix)
          (c2, p2) <- zipWithPosition(matrix)
        } yield distance(p1, p2) * c1 * c2).sum

      def normalisation = matrix.length / math.sqrt(math.Pi)

      (numerator / (totalQuantity * totalQuantity)) / normalisation
    }

    def distance(p1: (Int,Int), p2: (Int,Int)): Double = {
      val (i1, j1) = p1
      val (i2, j2) = p2
      val a = i2 - i1
      val b = j2 - j1
      math.sqrt(a * a + b * b)
    }

    def zipWithPosition(m :Seq[Seq[Double]]): Seq[(Double, (Int,Int))] = {
      for {
        (row, i) <- m.zipWithIndex
        (content, j) <- row.zipWithIndex
      } yield content ->(i, j)
    }


    def entropy(matrix: Seq[Seq[Double]]) = {
      val totalQuantity = matrix.flatten.sum
      assert(totalQuantity > 0)
      matrix.flatten.map {
        p =>
          val quantityRatio = p/ totalQuantity
          val localEntropy = if (quantityRatio == 0.0) 0.0 else quantityRatio * math.log(quantityRatio)
          //assert(!localEntropy.isNaN, s"${quantityRatio} ${math.log(quantityRatio)}")
          localEntropy
      }.sum * (-1 / math.log(matrix.flatten.length))
    }



    def moran(matrix: Seq[Seq[Double]]): Double = {
      def flatCells = matrix.flatten
      val totalPop = flatCells.sum
      val averagePop = totalPop / matrix.flatten.length


      def vals =
        for {
          (c1, p1) <- zipWithPosition(matrix)
          (c2, p2) <- zipWithPosition(matrix)
        } yield (decay(p1, p2) * (c1 - averagePop) * (c2 - averagePop),decay(p1, p2))



      def numerator : Double = vals.map{case (n,_)=>n}.sum
      def totalWeight : Double = vals.map{case(_,w)=>w}.sum

      def denominator =
        flatCells.map {
          p =>
            if (p == 0) 0
            else math.pow(p - averagePop.toDouble, 2)
        }.sum

      if (denominator == 0) 0
      else (matrix.flatten.length / totalWeight) * (numerator / denominator)
    }

    def decay(p1:(Int,Int),p2:(Int,Int)) = {
      if (p1==p2) 0.0
      else 1/distance(p1,p2)
    }



      /**
       * Moran index using fast convolution.
       *
       * @param matrix
       * @return
       */
      def moran_convol(matrix: Seq[Seq[Double]]): Double = {
        val conf = matrix.map { row => row.toArray }.toArray
        val n = conf.length
        val flatConf = conf.flatten
        val popMean = flatConf.sum / flatConf.length
        val centeredConf = conf.map { r => r.map { d => d - popMean } }
        val variance = MathArrays.ebeMultiply(centeredConf.flatten, centeredConf.flatten).sum
        val weights = spatialWeights(2 * n - 1)
        val totWeight = Convolution.convolution2D(Array.fill(n, n) { 1.0 }, weights).flatten.sum
        flatConf.length / (totWeight * variance) * MathArrays.ebeMultiply(centeredConf.flatten, Convolution.convolution2D(centeredConf, weights).flatten).sum
      }

      def spatialWeights(n: Int): Array[Array[Double]] = {
        Array.tabulate(n, n) { (i, j) => if (i == n / 2 && j == n / 2) 0.0 else 1 / math.sqrt((i - n / 2) * (i - n / 2) + (j - n / 2) * (j - n / 2)) }
      }

      /**
       * Mean distance using fast convolution.
       *
       * @param matrix
       * @return
       */
      def distance_convol(matrix: Seq[Seq[Double]]): Double = {
        val conf = matrix.map { row => row.toArray }.toArray
        val totPop = conf.flatten.sum
        val dmat = distanceMatrix(2 * conf.length - 1)
        val conv = Convolution.convolution2D(conf, dmat)
        math.sqrt(math.Pi) / (conf.length * totPop * totPop) * MathArrays.ebeMultiply(conv.flatten, conf.flatten).sum
      }

      /**
       * Distance kernel
       *
       * @param n
       * @return
       */
      def distanceMatrix(n: Int): Array[Array[Double]] = {
        Array.tabulate(n, n) { (i, j) => math.sqrt((i - n / 2) * (i - n / 2) + (j - n / 2) * (j - n / 2)) }
      }




}



/*
class SampleScalaExtension extends api.DefaultClassManager {
  def load(manager: api.PrimitiveManager) {
    manager.addPrimitive("first-n-integers", IntegerList)
    manager.addPrimitive("my-list", MyList)
    manager.addPrimitive("create-red-turtles", CreateRedTurtles)
  }
}

object IntegerList extends api.DefaultReporter {
  override def getSyntax =
    reporterSyntax(Array(NumberType), ListType)
  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val n = try args(0).getIntValue
    catch {
      case e: api.LogoException =>
        throw new api.ExtensionException(e.getMessage)
    }
    if (n < 0)
      throw new api.ExtensionException("input must be positive")
    (0 until n).toLogoList
  }
}

object MyList extends api.DefaultReporter {
  override def getSyntax =
    reporterSyntax(Array(WildcardType | RepeatableType), ListType, 2)
  def report(args: Array[api.Argument], context: api.Context) =
    args.map(_.get).toLogoList
}

object CreateRedTurtles extends api.DefaultCommand with nvm.CustomAssembled {
  override def getSyntax =
    commandSyntax(Array(NumberType, CommandBlockType | OptionalType))
  // the command itself is observer-only. inside the block is turtle code.
  override def getAgentClassString = "O:-T--"
  // only box this once
  private val red = Double.box(15)
  def perform(args: Array[api.Argument], context: api.Context) {
    // the api package doesn't have what we need, so we'll often
    // be dropping down to the agent and nvm packages
    val n = args(0).getIntValue
    val world = context.getAgent.world.asInstanceOf[agent.World]
    val eContext = context.asInstanceOf[nvm.ExtensionContext]
    val nvmContext = eContext.nvmContext
    val builder =
      new agent.AgentSetBuilder(api.AgentKind.Turtle, n)
    for(_ <- 0 until n) {
      val turtle = world.createTurtle(world.turtles)
      turtle.colorDoubleUnchecked(red)
      builder.add(turtle)
      eContext.workspace.joinForeverButtons(turtle)
    }
    // if the optional command block wasn't supplied, then there's not
    // really any point in calling this, but it won't bomb, either
    nvmContext.runExclusiveJob(builder.build(), nvmContext.ip + 1)
    // prim._extern will take care of leaving nvm.Context ip in the right place
  }
  def assemble(a: nvm.AssemblerAssistant) {
    a.block()
    a.done()
  }
}

*/
