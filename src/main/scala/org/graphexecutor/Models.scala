package org.graphexecutor

import scalala.tensor.dense.DenseMatrix
import scalala.tensor.{ Tensor, Tensor1, Tensor2, Vector, Matrix };
import scalala.tensor.dense.{ DenseVector, DenseMatrix };
import scalala.tensor.sparse._
//import scalala.tensor.operators._
import scalala.library._
import scalala.library.random._
import scalala.tensor.::;
import scalala.tensor._;
import scalala.tensor.dense._;

class Model {
  def solve = {}
}

/**
 * Linear system model
 * b = Ax
 * solve for x
 */
class LinearModel( size: Int ) extends Model {

  var b = DenseVector.rand( size )
  var x = DenseVector.rand( size )
  var A = DenseMatrix.rand( size, size )

  override def solve = {
    x = A \ b
  }
}

object LinearModel {
  def apply( size: Int ) = new LinearModel( size )
  def apply() = new LinearModel( 20 )
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
class SSsystem( statesize: Int, inputsize: Int, outputsize: Int, dt: Double ) extends Model {

  var xdot = DenseVector.rand( statesize )
  var x = DenseVector.rand( statesize )
  var u = DenseVector.rand( inputsize )
  var y = DenseVector.rand( outputsize )

  var A = DenseMatrix.rand( statesize, statesize )
  var B = DenseMatrix.rand( statesize, inputsize )
  var C = DenseMatrix.rand( statesize, statesize )
  var D = DenseMatrix.rand( statesize, outputsize )

  override def solve = {
    xdot = A * x + B * u
    x = x + xdot * dt
    y = C * x + D * u
  }
}

object SSsystem {
  def apply( size: Int, dt: Double ) = new SSsystem( size, size, size, dt )
  def apply( size: Int ) = new SSsystem( size, size, size, 0.1 )
  def apply() = new SSsystem( 20, 20, 20, 0.1 )
}