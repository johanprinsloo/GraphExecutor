 :
 :
/**
 * Linear system model
 * b = Ax
 * solve for x
 */
class LinearModel(size: Int) extends Model
{

  var b = rand(size).asInstanceOf[DenseVector]
  var x = rand(size).asInstanceOf[DenseVector]
  var A = rand(size, size).asInstanceOf[DenseMatrix]


  override def solve = {
     x = ( A \ b ).value
   }
}

/**
 * General linear state space model
 * defined by:
 *   x = A*xdot + B*u
 *   y = C*xdot + D*u
 * where:
 *   x is the State Vector
 *   u the Disturbance Vector
 *   A is the State Matrix of derivatives
 *   B the input Connection Matrix
 *   C is the output connection matrix
 *   D is the feedthrough matrix
 *
 */
class SSsystem(statesize: Int, inputsize: Int, outputsize: Int, dt: Double) extends Model
{

  var xdot = rand(statesize).asInstanceOf[DenseVector]
  var x = rand(statesize).asInstanceOf[DenseVector]
  var u = rand(inputsize).asInstanceOf[DenseVector]
  var y = rand(outputsize).asInstanceOf[DenseVector]

  var A = rand(statesize, statesize).asInstanceOf[DenseMatrix]
  var B = rand(statesize, inputsize).asInstanceOf[DenseMatrix]
  var C = rand(statesize, statesize).asInstanceOf[DenseMatrix]
  var D = rand(statesize, outputsize).asInstanceOf[DenseMatrix]

  override def solve = {
     xdot = ( A * x ).value + ( B * u).value
     x = x + xdot*dt
     y = ( C * x ).value + (D * u).value
   }
}