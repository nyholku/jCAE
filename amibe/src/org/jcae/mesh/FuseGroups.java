/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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


package org.jcae.mesh;

import org.jcae.mesh.xmldata.MeshReader;
import java.util.logging.Logger;

/**
 * Reads a 3D mesh, performs computations and stores it back.
 */
public class FuseGroups
{
	private static final Logger logger=Logger.getLogger(FuseGroups.class.getName());

	public static void main(String args[])
	{
		try
		{
			if (args.length != 2)
			{
				System.out.println("Usage : FuseGroups output_directory group_file");
				System.exit(1);
			}
			logger.info("Run FuseGroups");
			String xmlDir = args[0];
			String filename=args[1];
			
			MeshReader.mergeGroups(xmlDir, filename);
			logger.info("End fuse");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
