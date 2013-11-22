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
/*
 * Created Mon Nov 14 13:28:34 GMT-03:00 2005 by MyEclipse Hibernate Tool.
 */
package br.gov.jfrj.siga.ex;

import java.io.Serializable;

import br.gov.jfrj.siga.model.Selecionavel;

/**
 * A class that represents a row in the 'EX_FORMA_DOCUMENTO' table. This class
 * may be customized as it is never re-generated after being created.
 */
public class ExFormaDocumento extends AbstractExFormaDocumento implements
		Serializable, Selecionavel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8863713030171351063L;

	/**
	 * Simple constructor of ExFormaDocumento instances.
	 */
	public ExFormaDocumento() {
	}

	/**
	 * Constructor of ExFormaDocumento instances given a simple primary key.
	 * 
	 * @param idFormaDoc
	 */
	public ExFormaDocumento(final java.lang.Integer idFormaDoc) {
		super(idFormaDoc);
	}

	public Long getId() {
		return Long.parseLong(getIdFormaDoc().toString());
	}

	public String getSigla() {
		return getSiglaFormaDoc();
	}

	public void setSigla(final String sigla) {
		setSiglaFormaDoc(sigla);

	}

	public String getDescricao() {
		return getDescrFormaDoc();
	}

	public Boolean isNumeracaoUnica() {
		return getExTipoFormaDoc().getNumeracaoUnica() != 0;
	}

	public String toString(){
		return this.getDescricao();
	}
	/* Add customized code below */
}
