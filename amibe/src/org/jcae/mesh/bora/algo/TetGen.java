/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2006, by EADS CRC
 
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

package org.jcae.mesh.bora.algo;

import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.bora.xmldata.BinaryReader;
import org.jcae.mesh.xmldata.UNVConverter;
import org.jcae.mesh.xmldata.MeshWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.log4j.Logger;

/**
 * Run the Tetgen 3D mesher.
 * TetGen is Copyright 2002, 2004, 2005, 2006 Hang Si
 * Rathausstr. 9, 10178 Berlin, Germany
 * si@wias-berlin.de
 * This is *not* a free software and can not be redistributed
 * in jCAE.
 */
public class TetGen implements AlgoInterface
{
	private static Logger logger=Logger.getLogger(TetGen.class);
	private static final String tetgenCmd = "tetgen";
	private double volume;
	private static boolean available = true;
	private static String banner = null;

	public TetGen(double len)
	{
		// Max volume
		volume = 5.0*len*len*len;
		if (banner == null)
		{
			available = true;
			banner = "";
			try {
				Process p = Runtime.getRuntime().exec(new String[] {tetgenCmd, "-version"});
				p.waitFor();
				if (p.exitValue() != 0)
					available = false;
				else
				{
					String line;
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while ((line = input.readLine()) != null)
						banner += line;
					input.close();
				}
			} catch (Exception ex) {
				available = false;
			}
		}
	}

	public boolean isAvailable()
	{
		return available;
	}

	public int getOrientation(int o)
	{
		return o;
	}

	public boolean compute(BCADGraphCell mesh)
	{
		logger.info("Running TetGen "+banner);
		// mesh.export(s, "tetgen.poly", ExportMesh.FORMAT_POLY);
		Mesh m = BinaryReader.readObject(mesh);
		MeshWriter.writeObject3D(m, "tetgen.tmp", "jcae3d", "brep", mesh.getGraph().getModel().getCADFile(), 1);
		new UNVConverter("tetgen.tmp").writePOLY("tetgen.poly");
		try {
			Process p = Runtime.getRuntime().exec(new String[] {tetgenCmd, "-a"+volume+"pYNEFg", "tetgen"});
			p.waitFor();
			if (p.exitValue() != 0)
				return false;
			File temp = new File(".", "tetgen.1.mesh");
			File output = new File(".", "tetgen."+mesh.getId()+".mesh");
			if (output.exists())
				output.delete();
			temp.renameTo(output);
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	public String toString()
	{
		String ret = "Algo: "+getClass().getName();
		ret += "\n"+banner;
		ret += "\nMax volume: "+volume;
		return ret;
	}
}
