/*******************************************************************************
 * Copyright (c) 2006 - 2011 SJRJ.
 * 
 *     This file is part of SIGA.
 * 
 *     SIGA is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     SIGA is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with SIGA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package br.gov.jfrj.siga.sinc.lib;

import java.util.ArrayList;

public class OperacaoList extends ArrayList<Item> {

	@Override
	public String toString() {
		String s = "";
		for (Item i : this) {
			s += i.getOperacao()
					+ " - "
					+ (i.getNovo() == null ? "null" : i.getNovo()
							.getIdExterna())
					+ " - "
					+ (i.getAntigo() == null ? "null" : i.getAntigo()
							.getIdExterna()) + "\n";
		}
		return s;
	}

}
