/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006  EADS CRC

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

package org.jcae.mesh.amibe.ds;

import java.util.ArrayList;
import java.util.Iterator;
import java.io.Serializable;
import org.apache.log4j.Logger;

public class HalfEdge implements Serializable
{
	private static Logger logger = Logger.getLogger(HalfEdge.class);
	private Triangle tri;
	private byte localNumber = 8;
	private int attributes = 8;
	// For non manifold edges, a virtual triangle is added
	//   Triangle(outerVertex, edge.origin(), edge.destination())
	// and sym points to an edge of this triangle.  It is said to
	// be outer.  The list of adjacent HalfEdge is stored in this
	// triangle, more specifically in sym.next.sym
	// This is very handy because all HalfEdge of non-outer triangles
	// can be considered as being manifold.
	// TODO: replace ArrayList by HalfEdge[]
	private Object sym = null;
	private HalfEdge next = null;

	private static final int [] next3 = { 1, 2, 0 };
	private static final int [] prev3 = { 2, 0, 1 };
	
	public HalfEdge(Triangle tri, byte localNumber, byte attributes)
	{
		this.tri = tri;
		this.localNumber = localNumber;
		this.attributes = attributes;
	}
	
	/**
	 * Return the triangle tied to this object.
	 *
	 * @return the triangle tied to this object.
	 */
	public final Triangle getTri()
	{
		return tri;
	}
	
	/**
	 * Return the edge local number.
	 *
	 * @return the edge local number.
	 */
	public final int getLocalNumber()
	{
		return localNumber;
	}
	
	/**
	 * Set the edge tied to this object.
	 *
	 * @param s  the edge tied to this object.
	 */
	public final void glue(HalfEdge s)
	{
		sym = s;
		if (s == null)
			return;
		tri.glue1(localNumber, s.tri, s.localNumber);
		s.sym = this;
		s.tri.glue1(s.localNumber, tri, localNumber);
	}
	
	public final HalfEdge notOriented()
	{
		assert sym instanceof HalfEdge;
		if (sym != null && sym.hashCode() < hashCode())
			return (HalfEdge) sym;
		return this;
	}
	
	/**
	 * Get the symmetric edge.
	 */
	public final Object getAdj()
	{
		return sym;
	}

	/**
	 * Set the sym link.
	 */
	public final void setAdj(Object e)
	{
		sym = e;
	}

	public final HalfEdge sym()
	{
		return (HalfEdge) sym;
	}

	/**
	 * Move to the next edge.
	 */
	public final HalfEdge next()
	{
		return next;
	}
	
	/**
	 * Move to the previous edge.
	 */
	public final HalfEdge prev()
	{
		return next.next;
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same origin.
	 */
	public final HalfEdge nextOrigin()
	{
		return prev().sym();
	}
	
	/**
	 * Move counterclockwaise to the previous edge with the same origin.
	 */
	public final HalfEdge prevOrigin()
	{
		return sym().next;
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same
	 * destination.
	 */
	public final HalfEdge nextDest()
	{
		return sym().prev();
	}
	
	/**
	 * Move counterclockwaise to the previous edge with the same
	 * destination.
	 */
	public final HalfEdge prevDest()
	{
		return next.sym();
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same apex.
	 */
	public final HalfEdge nextApex()
	{
		return next.sym().next;
	}
	
	/**
	 * Move clockwaise to the previous edge with the same apex.
	 */
	public final HalfEdge prevApex()
	{
		return prev().sym().prev();
	}
	
	//  The following 3 methods change the underlying triangle.
	//  So they also modify all HalfEdge bound to this one.
	/**
	 * Sets the start vertex of this edge.
	 *
	 * @param v  the start vertex of this edge.
	 */
	public final void setOrigin(Vertex v)
	{
		tri.vertex[next3[localNumber]] = v;
	}
	
	/**
	 * Sets the end vertex of this edge.
	 *
	 * @param v  the end vertex of this edge.
	 */
	public final void setDestination(Vertex v)
	{
		tri.vertex[prev3[localNumber]] = v;
	}
	
	/**
	 * Sets the apex of this edge.
	 *
	 * @param v  the apex of this edge.
	 */
	public final void setApex(Vertex v)
	{
		tri.vertex[localNumber] = v;
	}
	
	/**
	 * Set the next link.
	 */
	public final void setNext(HalfEdge e)
	{
		next = e;
	}
	
	/**
	 * Check if some attributes of this edge are set.
	 *
	 * @param attr  the attributes to check
	 * @return <code>true</code> if this HalfEdge has all these
	 * attributes set, <code>false</code> otherwise.
	 */
	public final boolean hasAttributes(int attr)
	{
		return (attributes & attr) != 0;
	}
	
	/**
	 * Set attributes of this edge.
	 *
	 * @param attr  the attribute of this edge.
	 */
	public final void setAttributes(int attr)
	{
		attributes |= attr;
		pushAttributes();
	}
	
	/**
	 * Reset attributes of this edge.
	 *
	 * @param attr   the attributes of this edge to clear out.
	 */
	public final void clearAttributes(int attr)
	{
		attributes &= ~attr;
		pushAttributes();
	}
	
	// Adjust tri.adjPos after attributes is modified.
	public final void pushAttributes()
	{
		tri.setEdgeAttributes(localNumber, attributes);
	}
	
	// Adjust attributes after tri.adjPos is modified.
	public final void pullAttributes()
	{
		attributes = (byte) tri.getEdgeAttributes(localNumber);
	}
	
	/**
	 * Returns the start vertex of this edge.
	 *
	 * @return the start vertex of this edge.
	 */
	public final Vertex origin()
	{
		return tri.vertex[next3[localNumber]];
	}
	
	/**
	 * Returns the end vertex of this edge.
	 *
	 * @return the end vertex of this edge.
	 */
	public final Vertex destination()
	{
		return tri.vertex[prev3[localNumber]];
	}
	
	/**
	 * Returns the apex of this edge.
	 *
	 * @return the apex of this edge.
	 */
	public final Vertex apex()
	{
		return tri.vertex[localNumber];
	}
	
	public final void copyOTriangle(OTriangle ot)
	{
		ot.tri = tri;
		ot.localNumber = localNumber;
		ot.pullAttributes();
	}
	
	/**
	 * Return the HalfEdge instance corresponding to a given OTriangle.
	 *
	 * @param ot an OTriangle instance.
	 * @return the HalfEdge instance corresponding to ot.
	 */
	public final static HalfEdge toHalfEdge(OTriangle ot)
	{
		HalfEdge edge = ot.getTri().getHalfEdge();
		assert edge != null : ot;
		for (int j = ot.getLocalNumber(); j > 0; j--)
			edge = edge.next;
		return edge;
	}
	
	/**
	 * Find the <code>HalfEdge</code> joining two given vertices.
	 *
	 * @param v1  start point of the desired <code>HalfEdge</code>
	 * @param v2  end point of the desired <code>HalfEdge</code>
	 * @return <code>null</code> if vertices do not share a common
	 *         edge, otherwise the <code>HalfEdge</code> instance.
	 */
	public static HalfEdge find(Vertex v1, Vertex v2)
	{
		HalfEdge ret = ((Triangle) v1.getLink()).getHalfEdge();
		if (ret == null)
			return null;
		if (ret.destination() == v1)
			ret = ret.next();
		else if (ret.apex() == v1)
			ret = ret.prev();
		assert ret.origin() == v1 : v1+" not in "+ret.getTri();
		Vertex d = ret.destination();
		if (d == v2)
			return ret;
		do
		{
			ret = ret.nextOriginLoop();
			if (ret.destination() == v2)
				return ret;
		}
		while (ret.destination() != d);
		return null;
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same origin.
	 * If a boundary is reached, loop backward until another
	 * boundary is found and start again from there.
	 */
	public final HalfEdge nextOriginLoop()
	{
		HalfEdge ret = this;
		if (ret.hasAttributes(OTriangle.OUTER) && ret.hasAttributes(OTriangle.BOUNDARY | OTriangle.NONMANIFOLD))
		{
			// Loop clockwise to another boundary
			// and start again from there.
			do
			{
				ret = ret.prevOrigin();
			}
			while (!ret.hasAttributes(OTriangle.OUTER));
		}
		else
			ret = ret.nextOrigin();
		return ret;
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same apex.
	 * If a boundary is reached, loop backward until another
	 * boundary is found and start again from there.
	 */
	public final HalfEdge nextApexLoop()
	{
		HalfEdge ret = this;
		if (ret.hasAttributes(OTriangle.OUTER) && ret.next.next.hasAttributes(OTriangle.BOUNDARY | OTriangle.NONMANIFOLD))
		{
			// Loop clockwise to another boundary
			// and start again from there.
			do
			{
				ret = ret.prevApex();
			}
			while (!ret.hasAttributes(OTriangle.OUTER));
		}
		else
			ret = ret.nextApex();
		return ret;
	}
	
	/**
	 * Swaps an edge.
	 *
	 * This routine swaps an edge (od) to (na).  (on) is returned
	 * instead of (na), because this helps turning around o, eg.
	 * at the end of {@link org.jcae.mesh.amibe.patch.OTriangle2D#split3}.
	 *        
	 *          d                    d
	 *          .                    .
	 *         /|\                  / \
	 *        / | \                /   \   
	 *       /  |  \              /     \
	 *    a +   |   + n  ---&gt;  a +-------+ n
	 *       \  |  /              \     /
	 *        \ | /                \   /
	 *         \|/                  \ /
	 *          '                    '
	 *          o                    o
	 */
	public final HalfEdge swap()
	{
		Vertex o = origin();
		Vertex d = destination();
		Vertex a = apex();
		/*
		 *            d                    d
		 *            .                    .
		 *           /|\                  / \
		 *       s0 / | \ s3         s0  /   \ s3
		 *         /  |  \              / T2  \
		 *      a + T1|T2 + n  --->  a +-------+ n
		 *         \  |  /              \ T1  /
		 *       s1 \ | / s2         s1  \   / s2
		 *           \|/                  \ /
		 *            '                    '
		 *            o                    o
		 */
		// T1 = (oda)  --> (ona)
		// T2 = (don)  --> (dan)
		assert !hasAttributes(OTriangle.OUTER | OTriangle.BOUNDARY | OTriangle.NONMANIFOLD);
		HalfEdge [] e = new HalfEdge[6];
		e[4] = this;
		e[0] = next;
		e[1] = next.next;
		e[5] = sym();
		e[2] = sym().next;
		e[3] = sym().next.next;
		HalfEdge [] s = new HalfEdge[4];
		for (int i = 0; i < 4; i++)
			s[i] = e[i].sym();
		//  Clear SWAPPED flag for all edges of the 2 triangles
		for (int i = 0; i < 6; i++)
			e[i].clearAttributes(OTriangle.SWAPPED);
		for (int i = 0; i < 4; i++)
			s[i].clearAttributes(OTriangle.SWAPPED);
		//  Adjust vertices
		Vertex n = e[5].apex();
		e[4].setDestination(n);           // (ona)
		e[5].setDestination(a);           // (dan)
		//  Adjust edge informations
		//    T1: e[1] is unchanged
		Triangle T1 = e[1].tri;
		e[1].next = e[2];
		e[2].next = e[4];
		e[4].next = e[1];
		e[2].tri = e[4].tri = T1;
		e[2].localNumber = (byte) next3[e[1].localNumber];
		e[4].localNumber = (byte) prev3[e[1].localNumber];
		//    T2: e[3] is unchanged
		Triangle T2 = e[3].tri;
		e[3].next = e[0];
		e[0].next = e[5];
		e[5].next = e[3];
		e[0].tri = e[5].tri = T2;
		e[0].localNumber = (byte) next3[e[3].localNumber];
		e[5].localNumber = (byte) prev3[e[3].localNumber];
		//  Update attributes
		for (int i = 0; i < 4; i++)
			e[i].pushAttributes();
		//  Adjust edge pointers of triangles
		if (e[1].localNumber == 1)
			T1.setHalfEdge(e[4]);
		else if (e[1].localNumber == 2)
			T1.setHalfEdge(e[2]);
		if (e[3].localNumber == 1)
			T2.setHalfEdge(e[5]);
		else if (e[3].localNumber == 2)
			T2.setHalfEdge(e[0]);
		//  Glue edges to update triangle informations
		for (int i = 0; i < 4; i++)
			e[i].glue(s[i]);
		e[4].glue(e[5]);
		//  Mark new edges
		e[4].attributes = 0;
		e[5].attributes = 0;
		e[4].setAttributes(OTriangle.SWAPPED);
		e[5].setAttributes(OTriangle.SWAPPED);
		//  Fix links to triangles
		o.setLink(e[1].tri);
		d.setLink(e[3].tri);
		// Be consistent with OTriangle.swap()
		return e[2];
	}
	
	/**
	 * Contract an edge.
	 * TODO: Attributes are not checked.
	 * @param n the resulting vertex
	 */
	public final void contract(Mesh m, Vertex n)
	{
		Vertex o = origin();
		Vertex d = destination();
		if (logger.isDebugEnabled())
			logger.debug("contract ("+o+" "+d+")\ninto "+n);
		/*
		 *           V1                       V1
		 *  V3+-------+-------+ V4   V3 +------+------+ V4
		 *     \ t3  / \ t4  /           \  t3 | t4  / 
		 *      \   /   \   /              \   |   /
		 *       \ / t1  \ /                 \ | /  
		 *      o +-------+ d   ------>      n +
		 *       / \ t2  / \                 / | \
		 *      /   \   /   \              /   |   \
		 *     / t5  \ / t6  \           /  t5 | t6  \
		 *    +-------+-------+         +------+------+
		 *  V5        V2       V6     V5       V2      V6
		 */
		// this = (odV1)
		
		//  Replace o by n in all incident triangles
		HalfEdge e, f, s;
		e = this;
		do
		{
			e.setOrigin(n);
			e = e.nextOriginLoop();
		}
		while (e.destination() != d);
		//  Replace d by n in all incident triangles
		e = e.sym();
		do
		{
			e.setOrigin(n);
			e = e.nextOriginLoop();
		}
		while (e.destination() != n);
		//  Update adjacency links.  For clarity, o and d are
		//  written instead of n.
		e = next();             // (dV1o)
		int attr4 = e.attributes;
		s = e.sym();            // (V1dV4)
		e = e.next();           // (V1od)
		int attr3 = e.attributes;
		f = e.sym();            // (oV1V3)
		if (f != null)
			f.glue(s);
		else if (s != null)
			s.glue(null);
		if (f != null)
		{
			f.attributes |= attr4;
			f.pushAttributes();
		}
		if (s != null)
		{
			s.attributes |= attr3;
			s.pushAttributes();
		}
		if (!hasAttributes(OTriangle.OUTER))
		{
			Triangle t34 = f.tri;
			if (t34.isOuter())
				t34 = s.tri;
			assert !t34.isOuter() : s+"\n"+f;
			f.destination().setLink(t34);
			n.setLink(t34);
		}
		e = e.next();                   // (odV1)
		e = e.sym();                    // (doV2)
		e = e.next();                   // (oV2d)
		int attr5 = e.attributes;
		s = e.sym();                    // (V2oV5)
		e = e.next();                   // (V2do)
		int attr6 = e.attributes;
		f = e.sym();                    // (dV2V6)
		if (f != null)
			f.glue(s);
		else if (s != null)
			s.glue(null);
		if (f != null)
		{
			f.attributes |= attr5;
			f.pushAttributes();
		}
		if (s != null)
		{
			s.attributes |= attr6;
			s.pushAttributes();
		}
		if (!e.hasAttributes(OTriangle.OUTER))
		{
			Triangle t56 = s.tri;
			if (t56.isOuter())
				t56 = f.tri;
			assert !t56.isOuter();
			s.origin().setLink(t56);
			n.setLink(t56);
		}
		e = e.next();                   // (doV2)
		// Must be called before T2 is removed
		s = e.sym();
		e.clearAttributes(OTriangle.MARKED);
		e.pushAttributes();
		// Remove T2
		m.remove(e.tri);
		s.clearAttributes(OTriangle.MARKED);
		s.pushAttributes();
		// Remove T1
		m.remove(s.tri);
	}
	
	/**
	 * Split an edge.  This is the opposite of contract.
	 *
	 * @param n the resulting vertex
	 */
	public final void split(Mesh m, Vertex n)
	{
		/*
		 *            V1                             V1
		 *            /'\                            /|\
		 *          /     \                        /  |  \
		 *        /      h1 \                    /    |    \
		 *      /             \                /    n1|   h1 \
		 *    /       t1        \            /   t1   |  t3    \
		 * o +-------------------+ d ---> o +---------+---------+ d
		 *    \       t2        /            \   t2   |  t4    /
		 *      \             /                \    n2|   h2 /
		 *        \      h2 /                    \    |    /
		 *          \     /                        \  |  /
		 *            \,/                            \|/
		 *            V2                             V2
		 */
		HalfEdge h1 = next();        // (dV1o)
		HalfEdge h2 = sym().prev();  // (v2do)
		OTriangle ot = new OTriangle();
		copyOTriangle(ot);
		Triangle t1 = ot.getTri();
		ot.symOTri();                // (dov2)
		Triangle t2 = ot.getTri();
		ot.symOTri();                // (odv1)
		ot.split(m, n);              // (onv1)
		// HalfEdges have to be updated on 4 triangles.
		t1 = ot.getTri();
		HalfEdge n1 = new HalfEdge(t1, (byte) next3[ot.getLocalNumber()], (byte) 0);
		n1.pullAttributes();
		n1.next = h1.next;
		assert h1.next.next.next == h1;
		h1.next.next.next = n1;
		if (t1.getHalfEdge() == h1)
			t1.setHalfEdge(n1);
		ot.symOTri();                // (nov2)
		t2 = ot.getTri();
		HalfEdge n2 = new HalfEdge(t2, (byte) prev3[ot.getLocalNumber()], (byte) 0);
		n2.pullAttributes();
		n2.next = h2.next;
		assert h2.next.next.next == h2;
		h2.next.next.next = n2;
		if (t2.getHalfEdge() == h2)
			t2.setHalfEdge(n2);
		ot.symOTri();                // (onv1)
		HalfEdge [] e = new HalfEdge[3];
		ot.prevOTriDest();           // (v1nd)
		Triangle t3 = ot.getTri();
		h1.tri = t3;
		for (int i = 0; i < 3; i++)
		{
			if (i != h1.localNumber)
			{
				e[i] = new HalfEdge(t3, (byte) i, (byte) 0);
				e[i].pullAttributes();
			}
		}
		e[h1.localNumber] = h1;
		h1.pullAttributes();
		h1.next = e[next3[h1.localNumber]];
		e[next3[h1.localNumber]].next = e[prev3[h1.localNumber]];
		e[prev3[h1.localNumber]].next = h1;
		h1.next.glue(n1);
		t3.setHalfEdge(e[0]);

		ot.prevOTriDest();           // (dnv2)
		Triangle t4 = ot.getTri();
		h2.tri = t4;
		for (int i = 0; i < 3; i++)
		{
			if (i != h2.localNumber)
			{
				e[i] = new HalfEdge(t4, (byte) i, (byte) 0);
				e[i].pullAttributes();
			}
		}
		e[h2.localNumber] = h2;
		h2.pullAttributes();
		h2.next = e[next3[h2.localNumber]];
		e[next3[h2.localNumber]].next = e[prev3[h2.localNumber]];
		e[prev3[h2.localNumber]].next = h2;
		h2.next.next.glue(n2);
		t4.setHalfEdge(e[0]);
		h2.next.glue(h1.next.next);
		n2.next.glue(n1.next.next);
	}
	
	public String toString()
	{
		StringBuffer r = new StringBuffer();
		r.append("hashCode: "+hashCode());
		r.append("\nTriangle: "+tri.hashCode());
		r.append("\nLocal number: "+localNumber);
		if (sym != null)
		{
			if (sym instanceof HalfEdge)
			{
				HalfEdge e = (HalfEdge) sym;
				r.append("\nSym: "+e.tri.hashCode()+"["+e.localNumber+"]");
			}
			else
			{
				ArrayList list = (ArrayList) sym;
				r.append("\nSym: [");
				for (Iterator it = list.iterator(); it.hasNext(); )
				{
					HalfEdge e = (HalfEdge) it.next();
					r.append(e.tri.hashCode()+"["+e.localNumber+"]");
				}
				r.append("]");
			}
		}
		r.append("\nAttributes: "+Integer.toHexString(attributes));
		r.append("\nVertices:");
		r.append("\n  Origin: "+origin());
		r.append("\n  Destination: "+destination());
		r.append("\n  Apex: "+apex());
		return r.toString();
	}

	private static void unitTestBuildMesh(Mesh m, Vertex [] v)
	{
		/*
		 *                       v2
		 *                       +
		 *  Initial            / |
		 *  triangulation    /   |
		 *                 /     |
		 *               /       |
		 *             +---------+
		 *             v0        v1
		 *
		 * Final result:
		 *  v4        v3        v2
		 *   +---------+---------+
		 *   | \       |       / |
		 *   |   \     |     /   |
		 *   |     \   |   /     |
		 *   |       \ | /       |
		 *   +---------+---------+
		 *   v5        v0       v1
		 */
		System.out.println("Building mesh...");
		Triangle T = new Triangle(v[0], v[1], v[2]);
		m.add(T);
		assert m.isValid();
		Vertex [] init = new Vertex[3];
		System.arraycopy(v, 0, init, 0, 3);
		m.buildAdjacency(init, -1.0);
		assert m.isValid();
		m.buildEdges();
		assert m.isValid();
		HalfEdge e = T.getHalfEdge().next(); // (v2,v0,v1)
		e.split(m, v[3]); // (v2,v3,v1)
		assert m.isValid();
		e = e.next();     // (v3,v1,v2)
		e = e.swap();     // (v3,v0,v2)
		assert m.isValid();
		/*
		 *            v3        v2
		 *             +---------+
		 *             |       / |
		 *             |     /   |
		 *             |   /     |
		 *             | /       |
		 *             +---------+
		 *             v0       v1
		 */
		e.split(m, v[5]); // (v3,v5,v2)
		assert m.isValid();
		e = e.next();     // (v5,v2,v3)
		e = e.swap();     // (v5,v0,v3)
		assert m.isValid();
		/*
		 *            v3        v2
		 *             +---------+
		 *           / |       / |
		 *         /   |     /   |
		 *       /     |   /     |
		 *     /       | /       |
		 *   +---------+---------+
		 *   v5        v0       v1
		 */
		e = e.prev();     // (v3,v5,v0)
		e.split(m, v[4]); // (v3,v4,v0)
		assert m.isValid();
		/*
		 *  v4        v3        v2
		 *   +---------+---------+
		 *   | \       |       / |
		 *   |   \     |     /   |
		 *   |     \   |   /     |
		 *   |       \ | /       |
		 *   +---------+---------+
		 *   v5        v0       v1
		 */
	}
	
	private static void unitTestCheckLoopOrigin(Mesh m, Vertex o, Vertex d)
	{
		HalfEdge e = HalfEdge.find(o, d);
		if (e == null)
		        System.exit(-1);
		System.out.println("Loop around origin: "+o);
		System.out.println(" first destination: "+d);
		int cnt = 0;
		do
		{
			e = e.nextOriginLoop();
			cnt++;
		}
		while (e.destination() != d);
		assert cnt == 4 : "Failed test: LoopOrigin cnt != 4: "+o+" "+d;
	}
	
	private static void unitTestCheckToHalfEdge(Mesh m, Vertex o, Vertex d)
	{
		OTriangle ot = new OTriangle();
		if (!ot.find(o, d))
		        System.exit(-1);
		HalfEdge e = HalfEdge.toHalfEdge(ot);
		assert ot.origin() == e.origin() && ot.destination() == e.destination() && ot.apex() == e.apex();
	}
	
	private static void unitTestCheckContract(Mesh m, Vertex o, Vertex d, Vertex n)
	{
		HalfEdge e = HalfEdge.find(o, d);
		if (e == null)
		        System.exit(-1);
		e.contract(m, n);
		assert m.isValid();
	}
	
	public static void main(String args[])
	{
		Mesh m = new Mesh();
		Vertex [] v = new Vertex[6];
		v[0] = Vertex.valueOf(0.0, 0.0, 0.0);
		v[1] = Vertex.valueOf(1.0, 0.0, 0.0);
		v[2] = Vertex.valueOf(1.0, 1.0, 0.0);
		v[3] = Vertex.valueOf(0.0, 1.0, 0.0);
		v[4] = Vertex.valueOf(-1.0, 1.0, 0.0);
		v[5] = Vertex.valueOf(-1.0, 0.0, 0.0);
		unitTestBuildMesh(m, v);
		assert m.isValid();
		m.buildEdges();
		assert m.isValid();
		unitTestCheckToHalfEdge(m, v[0], v[1]);
		unitTestCheckToHalfEdge(m, v[1], v[0]);
		System.out.println("Checking loops...");
		unitTestCheckLoopOrigin(m, v[3], v[4]);
		unitTestCheckLoopOrigin(m, v[3], v[2]);
		unitTestCheckLoopOrigin(m, v[3], m.outerVertex);
		unitTestCheckContract(m, v[0], v[1], v[0]);
		unitTestCheckContract(m, v[5], v[0], v[0]);
		unitTestCheckContract(m, v[4], v[0], v[0]);
		assert m.isValid();
		System.out.println("Done.");

		System.out.println("Building non-manifold mesh...");
		m = new Mesh();
		Vertex [] nmv = new Vertex[6];
		nmv[0] = Vertex.valueOf(0.0, 0.0, 0.0);
		nmv[1] = Vertex.valueOf(0.0, 0.0, 3.0);
		nmv[2] = Vertex.valueOf(1.0, 0.0, 0.0);
		nmv[3] = Vertex.valueOf(0.0, 1.0, 0.0);
		nmv[4] = Vertex.valueOf(-1.0, 0.0, 0.0);
		nmv[5] = Vertex.valueOf(0.0, -1.0, 0.0);
		for (int i = 2; i < 6; i++)
		{
			Triangle T = new Triangle(nmv[0], nmv[1], nmv[i]);
			m.add(T);
			if (i == 2)
			{
				nmv[0].setLink(T);
				nmv[1].setLink(T);
			}
			nmv[i].setLink(T);
		}
		m.buildAdjacency(nmv, -1.0);
		m.printMesh();
		m.buildEdges();
		assert m.isValid();
		/*
		m = new Mesh();
		unitTestBuildMesh(m, v);
		m.buildEdges();
		HalfEdge e = HalfEdge.find(v[0], v[4]);
		HalfEdge s = e.swap();
		assert m.isValid();
		// m.printMesh();
		new org.jcae.mesh.amibe.algos3d.DecimateHalfEdge(m, 0.1).compute();
		*/
	}
}
