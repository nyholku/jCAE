/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

   (C) Copyright 2006, by EADS CRC

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */


package org.jcae.mesh.constraints;

import java.util.Iterator;
import java.util.Vector;
import org.apache.log4j.Logger;

public class Constraint extends Hypothesis
{
	private static Logger logger = Logger.getLogger(Constraint.class);
	protected int dimension = -1;
	protected boolean dirty = false;
	protected Object algo = null;
	private static Class [] innerClasses;
	static { try {
		innerClasses = Class.forName("org.jcae.mesh.constraints.Constraint").getDeclaredClasses();
	} catch (Exception ex) {ex.printStackTrace();}};

	// Is there a better way to do that?
	private void copyHypothesis(Hypothesis h, int d)
	{
		dimension   = d;
		lengthMin   = h.lengthMin;
		lengthMax   = h.lengthMax;
		deflection  = h.deflection;
		lengthBool  = h.lengthBool;
		numberBool  = h.numberBool;
		numberMin   = h.numberMin;
		numberMax   = h.numberMax;
		elementType = Constraint.impliedType(d, h.elementType);
		logger.debug("("+Integer.toHexString(h.hashCode())+") Dim: "+d+" Algo "+h.elementType+" mapped to "+elementType);
	}

	// Creates a Constraint derived from an Hypothesis
	private static Constraint createConstraint(Hypothesis h, int d)
	{
		Constraint ret = new Constraint();
		ret.copyHypothesis(h, d);
		if (ret.elementType == null)
			return null;
		return ret;
	}

	// Combines with an Hypothesis for a given dimension
	private void combine(MeshHypothesis mh, int d)
	{
		Constraint that = createConstraint(mh.getHypothesis(), d);
		if (that == null)
			return;
		if (dimension == -1)
			copyHypothesis(that, d);
		if (elementType != that.elementType)
		{
			logger.debug("Element "+elementType+" and "+that.elementType+" differ and are not combined together");
		}
		double targetLengthMax = lengthMax;
		if (targetLengthMax > that.lengthMax)
			targetLengthMax = that.lengthMax;
		double targetLengthMin = lengthMin;
		if (targetLengthMin < that.lengthMin)
			targetLengthMin = that.lengthMin;
		if (lengthBool && that.lengthBool)
		{
			if (targetLengthMin > targetLengthMax)
			{
				dirty = true;
				throw new RuntimeException("length min > length max");
			}
			lengthMax = targetLengthMax;
			lengthMin = targetLengthMin;
		}
		else
		{
			lengthBool |= that.lengthBool;
			if (targetLengthMin > targetLengthMax)
			{
				dirty = true;
				printDirty(mh);
				lengthMax = targetLengthMin;
				lengthMin = targetLengthMax;
			}
		}
		int targetNumberMax = numberMax;
		if (targetNumberMax > that.numberMax)
			targetNumberMax = that.numberMax;
		int targetNumberMin = numberMin;
		if (targetNumberMin < that.numberMin)
			targetNumberMin = that.numberMin;
		if (numberBool && that.numberBool)
		{
			if (targetNumberMin > targetNumberMax)
			{
				dirty = true;
				throw new RuntimeException("number min > number max");
			}
			numberMax = targetNumberMax;
			numberMin = targetNumberMin;
		}
		else
		{
			numberBool |= that.numberBool;
			if (targetNumberMin > targetNumberMax)
			{
				dirty = true;
				printDirty(mh);
				numberMax = targetNumberMin;
				numberMin = targetNumberMax;
			}
		}
		double targetDefl = deflection;
		if (targetDefl > that.deflection)
			targetDefl = that.deflection;
		deflection = targetDefl;
	}

	private void printDirty(MeshHypothesis mh)
	{
		logger.warn("Hypothesis not compatible: "+mh+": "+mh.getHypothesis()+" with "+this);
	}

	/**
	 * Combines all Hypothesis of a Vector.  In order to improve error
	 * reporting, MeshHypothesis objects are passed as arguments instead
	 * of Hypothesis.
	 *
	 * @param mh  list of MeshHypothesis objects.
	 * @param d   dimension
	 */
	public static Constraint combineAll(Vector mh, int d)
	{
		Constraint ret = null;
		if (mh.size() > 0)
		{
			ret = new Constraint();
			for (Iterator ita = mh.iterator() ; ita.hasNext(); )
				ret.combine((MeshHypothesis) ita.next(), d);
		}
		if (ret.dimension == -1)
			ret = null;
		return ret;
	}

	private static Hyp getAlgo(int d, String elt)
	{
		Hyp h = null;
		if (elt == null)
			return null;
		try {
			for (int i = 0; i < innerClasses.length; i++)
			{
				if (innerClasses[i].getName().equals("org.jcae.mesh.constraints.Constraint$Hyp"+elt))
					h = (Hyp) innerClasses[i].newInstance();
			}
		} catch (Exception ex) {ex.printStackTrace(); };
		return h;
	}

	private static String impliedType(int d, String elt)
	{
		Hyp h = getAlgo(d, elt);
		if (h == null)
			return null;
		return h.impliedType(d);
	}

	/**
	 * Finds the best algorithm suited to constraints defined on a submesh.
	 */
	public void findAlgorithm()
	{
		double targetLength = 0.5*(lengthMin+lengthMax);
		if (dimension == 1)
		{
			if (deflection > 0)
				algo = new String("org.jcae.mesh.mesher.algos1d.UniformLengthDeflection  length="+targetLength+"  deflection="+deflection);
			else
				algo = new String("org.jcae.mesh.mesher.algos1d.UniformLength  length="+targetLength);
		}
		else if (dimension == 2)
		{
			if (deflection > 0)
				algo = new String("org.jcae.mesh.amibe.algos2d.BasicMesh  length="+targetLength+"  deflection="+deflection);
			else
				algo = new String("org.jcae.mesh.amibe.algos2d.BasicMesh  length="+targetLength);
		}
		else if (dimension == 3)
		{
			algo = new String("org.jcae.mesh.amibe.algos3d.Netgen  length="+targetLength);
		}
	}

	private interface Hyp
	{
		public String impliedType(int d);
		public int dim();
	}

	public static class HypE2 implements Hyp
	{
		public int dim()
		{
			return 1;
		}
		public String impliedType(int d)
		{
			if (d == 1)
				return "E2";
			else
				return null;
		}
	}
	public static class HypT3 implements Hyp
	{
		public int dim()
		{
			return 2;
		}
		public String impliedType(int d)
		{
			if (d == 1)
				return "E2";
			else if (d == 2)
				return "T3";
			else
				return null;
		}
	}
	public static class HypT4 implements Hyp
	{
		public int dim()
		{
			return 3;
		}
		public String impliedType(int d)
		{
			if (d == 1)
				return "E2";
			else if (d == 2)
				return "T3";
			else if (d == 3)
				return "T4";
			else
				return null;
		}
	}
}
