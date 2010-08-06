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
 * (C) Copyright 2005-2009, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.XMLEncoder;
import java.io.IOException;
import org.jcae.netbeans.mesh.Mesh;
import org.openide.ErrorManager;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Node;

/**
 *
 * @author Gautam Botrel
 */
public class AmibeDataObject extends MultiDataObject implements SaveCookie
{
	public AmibeDataObject(FileObject arg0, MultiFileLoader arg1, Mesh mesh)
		throws DataObjectExistsException
	{
		super(arg0, arg1);
		this.mesh = mesh;
		mesh.addPropertyChangeListener(new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				setModified(true);
			}
		});
	}

	@Override
	protected Node createNodeDelegate()
	{
		return new AmibeNode(this);
	}

	private Mesh mesh;

	public Mesh getMesh()
	{
		return mesh;
	}

	public void save() throws IOException
	{
		FileLock l = null;
		XMLEncoder encoder = null;
		try
		{
			FileObject out = getPrimaryFile();
			l = out.lock();
			encoder = new XMLEncoder(out.getOutputStream(l));
			encoder.writeObject(mesh);
			setModified(false);
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
		finally
		{
			if(encoder!=null)
				encoder.close();
			if(l!=null)
				l.releaseLock();
		}
	}
}