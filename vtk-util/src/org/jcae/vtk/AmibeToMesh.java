/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2008, by EADS France
 */

package org.jcae.vtk;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author Julian Ibarz
 */
public class AmibeToMesh
{
	private Mesh mesh;

	public Mesh getMesh()
	{
		return mesh;
	}
	
	private static class GroupData extends LeafNode.DataProvider {
		private final AmibeProvider provider;
		private final int id;
		
		GroupData(AmibeProvider provider, int id)
		{
			this.provider = provider;
			this.id = id;
		}

		@Override
		public void load()
		{
			AmibeDomain domain = null;
			try {
			domain = new AmibeDomain(provider.getDirectory(), provider.getDocument(), id/*, Color.BLACK*/);
			}
			catch(IOException e)
			{
				System.err.println("Cannot load node " + id + 
					" from file " + provider.getDocument().getDocumentURI()
					+ e.getLocalizedMessage());
			}
			// Nodes
			super.setNodes(domain.getNodes());
			
			// Polys
			nbrOfPolys = domain.getTria3().length / 3 + domain.getQuad4().length / 4;
			int[] quads = domain.getQuad4();
			int[] triangles = domain.getTria3();
			
			int[] indices = new int[4 * (triangles.length / 3) +  5 * (quads.length / 4)];
			
			int offSet = 0;
			for(int i = 0 ; i < triangles.length ; )
			{
				indices[offSet++] = 3;
				indices[offSet++] = triangles[i++];
				indices[offSet++] = triangles[i++];
				indices[offSet++] = triangles[i++];
			}
			
			for(int i = 0 ; i < quads.length ; )
			{
				indices[offSet++] = 4;
				indices[offSet++] = triangles[i++];
				indices[offSet++] = triangles[i++];
				indices[offSet++] = triangles[i++];
				indices[offSet++] = triangles[i++];
			}
		}
	}
	
	public AmibeToMesh(String filePath, int[] groupExtraction)
		throws ParserConfigurationException, SAXException, IOException
	{
		AmibeProvider provider = new AmibeProvider(new File(filePath));
		mesh = new Mesh(groupExtraction.length);

		for(int id : groupExtraction)
			mesh.setGroup(id, new GroupData(provider, id));
	}
}
