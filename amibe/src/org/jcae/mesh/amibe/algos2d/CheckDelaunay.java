/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005,2006, by EADS CRC
    Copyright (C) 2007,2008,2009, by EADS France

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.algos2d;

import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.VirtualHalfEdge2D;
import org.jcae.mesh.amibe.patch.Vertex2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

/**
 * Swap edges which are not Delaunay.  In an Euclidian 2D metrics, there
 * is a unique Delaunay mesh, so edges can be processed in any order.
 * But with a Riemannian metrics this is no more true, edges with the
 * poorest quality should be processed first to improve the overall
 * quality.  This is not implemented yet, edges are currently not
 * sorted.
 */
public class CheckDelaunay
{
	private static final Logger LOGGER=Logger.getLogger(CheckDelaunay.class.getName());
	private final Mesh2D mesh;
	
	/**
	 * Creates a <code>CheckDelaunay</code> instance.
	 *
	 * @param m  the <code>CheckDelaunay</code> instance to check.
	 */
	public CheckDelaunay(Mesh2D m)
	{
		mesh = m;
	}
	
	private static final class FakeEdge
	{
		private final Triangle triangle;
		private final int localNumber;

		private FakeEdge(Triangle t, int l)
		{
			triangle = t;
			localNumber = l;
		}
	}

	/**
	 * Swap edges which are not Delaunay.
	 */
	public final void compute()
	{
		AbstractHalfEdge ot = null;
		AbstractHalfEdge sym = null;
		Vertex2D v;
		int cnt = 0;
		LOGGER.config("Enter compute()");
		mesh.pushCompGeom(3);

		boolean redo = false;
		int niter = mesh.getTriangles().size();
		Collection<Triangle> oldList = mesh.getTriangles();
		do {
			redo = false;
			cnt = 0;
			ArrayList<FakeEdge> toSwap = new ArrayList<FakeEdge>();
			Collection<Triangle> newList = new LinkedHashSet<Triangle>();
			niter--;
			for (Triangle t: oldList)
			{
				ot = t.getAbstractHalfEdge(ot);
				if (sym == null)
					sym = t.getAbstractHalfEdge(sym);
				for (int i = 0; i < 3; i++)
				{
					ot = ot.next();
					ot.clearAttributes(AbstractHalfEdge.SWAPPED);
					sym = ot.sym(sym);
					sym.clearAttributes(AbstractHalfEdge.SWAPPED);
				}
			}
			
			for (Triangle t: oldList)
			{
				ot = t.getAbstractHalfEdge(ot);
				ot = ot.prev();
				for (int i = 0; i < 3; i++)
				{
					ot = ot.next();
					if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD | AbstractHalfEdge.SHARP | AbstractHalfEdge.OUTER))
						continue;
					sym = ot.sym(sym);
					if (ot.hasAttributes(AbstractHalfEdge.SWAPPED) || sym.hasAttributes(AbstractHalfEdge.SWAPPED))
						continue;
					ot.setAttributes(AbstractHalfEdge.SWAPPED);
					sym.setAttributes(AbstractHalfEdge.SWAPPED);
					v = (Vertex2D) sym.apex();
					VirtualHalfEdge2D ot2 = new VirtualHalfEdge2D(t, i);
					if (!ot2.isDelaunay(mesh, v))
					{
						cnt++;
						toSwap.add(new FakeEdge(t, i));
					}
				}
			}
			LOGGER.fine(" Found "+cnt+" non-Delaunay triangles");
			for (FakeEdge e: toSwap)
			{
				ot = e.triangle.getAbstractHalfEdge(ot);
				for (int i = 0; i < e.localNumber; i++)
					ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.SWAPPED))
				{
					newList.add(ot.getTri());
					sym = ot.sym(sym);
					newList.add(sym.getTri());
					mesh.edgeSwap(ot);
					redo = true;
				}
			}
			//  The niter variable is introduced to prevent loops.
			//  With large meshes. its initial value may be too large,
			//  so we lower it now.
			if (niter > 10 * cnt)
				niter = 10 * cnt;
			oldList = newList;
		} while (redo && niter > 0);
		mesh.popCompGeom(3);
		LOGGER.config("Leave compute()");
	}
	
}
