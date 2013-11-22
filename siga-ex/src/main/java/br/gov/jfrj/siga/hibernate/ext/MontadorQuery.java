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
package br.gov.jfrj.siga.hibernate.ext;

import java.text.SimpleDateFormat;

import org.hibernate.Query;

import br.gov.jfrj.siga.dp.DpLotacao;
import br.gov.jfrj.siga.dp.DpPessoa;
import br.gov.jfrj.siga.ex.ExModelo;
import br.gov.jfrj.siga.hibernate.ExDao;

public class MontadorQuery implements IMontadorQuery {

	public String montaQueryConsultaporFiltro(final IExMobilDaoFiltro flt,
			DpPessoa titular, DpLotacao lotaTitular, boolean apenasCount) {

		StringBuffer sbf = new StringBuffer();

		if (apenasCount)
			sbf.append("select count(doc) from ExMarca label inner join label.exMobil mob inner join label.exMobil.exDocumento doc");
		else
			sbf.append("select doc, mob, label from ExMarca label inner join label.exMobil mob inner join mob.exDocumento doc");

		sbf.append(" where 1 = 1");

		if (flt.getUltMovIdEstadoDoc() != null
				&& flt.getUltMovIdEstadoDoc() != 0) {
			sbf.append(" and label.cpMarcador.idMarcador = :ultMovIdEstadoDoc");
		} else {
			sbf.append(" and not (label.cpMarcador.idMarcador in (3, 14, 25))");
		}

		if (flt.getUltMovRespSelId() != null && flt.getUltMovRespSelId() != 0) {
			sbf.append(" and label.dpPessoaIni.idPessoa = :ultMovRespSelId");
		}

		if (flt.getUltMovLotaRespSelId() != null
				&& flt.getUltMovLotaRespSelId() != 0) {
			sbf.append(" and label.dpLotacaoIni.idLotacao = :ultMovLotaRespSelId");
		}

		if (flt.getIdTipoMobil() != null && flt.getIdTipoMobil() != 0) {
			sbf.append(" and mob.exTipoMobil.idTipoMobil = :idTipoMobil");
		}

		if (flt.getNumSequencia() != null && flt.getNumSequencia() != 0) {
			sbf.append(" and mob.numSequencia = :numSequencia ");
		}

		if (flt.getIdOrgaoUsu() != null && flt.getIdOrgaoUsu() != 0) {
			sbf.append(" and doc.orgaoUsuario.idOrgaoUsu = :idOrgaoUsu");
		}

		if (flt.getAnoEmissao() != null && flt.getAnoEmissao() != 0) {
			sbf.append(" and doc.anoEmissao = :anoEmissao");
		}

		if (flt.getNumExpediente() != null && flt.getNumExpediente() != 0) {
			sbf.append(" and doc.numExpediente = :numExpediente");
		}

		if (flt.getIdTpDoc() != null && flt.getIdTpDoc() != 0) {
			sbf.append(" and doc.exTipoDocumento.idTpDoc = :idTpDoc");
		}

		if (flt.getIdFormaDoc() != null && flt.getIdFormaDoc() != 0) {
			sbf.append(" and doc.exFormaDocumento.idFormaDoc = :idFormaDoc");
		}

		if (flt.getIdTipoFormaDoc() != null && flt.getIdTipoFormaDoc() != 0) {
			sbf.append(" and doc.exFormaDocumento.exTipoFormaDoc.idTipoFormaDoc = :idTipoFormaDoc");
		}

		if (flt.getClassificacaoSelId() != null
				&& flt.getClassificacaoSelId() != 0) {
			sbf.append(" and doc.exClassificacao.idClassificacao = :classificacaoSelId");
		}

		if (flt.getDescrDocumento() != null
				&& !flt.getDescrDocumento().trim().equals("")) {
			sbf.append(" and upper(doc.descrDocumentoAI) like :descrDocumento");
		}

		// if (flt.getFullText() != null &&
		// !flt.getFullText().trim().equals("")) {
		// String s = flt.getFullText();
		// while (s.contains("  "))
		// s = s.replace("  ", " ");
		// s = s.replaceAll(" ", " AND ");
		// sbf.append(" and CONTAINS(conteudo_blob_doc, '");
		// sbf.append(s);
		// sbf.append("', 1) > 0");
		// }

		if (flt.getDtDoc() != null) {
			sbf.append(" and doc.dtDoc >= to_date(");
			sbf.append(":dtDoc");
			sbf.append(", 'dd/mm/yyyy')");
		}

		if (flt.getDtDocFinal() != null) {
			sbf.append(" and doc.dtDoc <= to_date(");
			sbf.append(":dtDocFinal");
			sbf.append(", 'dd/mm/yyyy')");
		}

		if (flt.getNumAntigoDoc() != null
				&& !flt.getNumAntigoDoc().trim().equals("")) {
			sbf.append(" and upper(doc.numAntigoDoc) like :numAntigoDoc");
		}

		if (flt.getDestinatarioSelId() != null
				&& flt.getDestinatarioSelId() != 0) {
			sbf.append(" and doc.destinatario.idPessoaIni = :destinatarioSelId");
		}

		if (flt.getLotacaoDestinatarioSelId() != null
				&& flt.getLotacaoDestinatarioSelId() != 0) {
			sbf.append(" and doc.lotaDestinatario.idLotacaoIni = :lotacaoDestinatarioSelId");
		}

		if (flt.getNmDestinatario() != null
				&& !flt.getNmDestinatario().trim().equals("")) {
			sbf.append(" and upper(doc.nmDestinatario) like :nmDestinatario");
		}

		if (flt.getCadastranteSelId() != null && flt.getCadastranteSelId() != 0) {
			sbf.append(" and doc.cadastrante.idPessoaIni = :cadastranteSelId");
		}

		if (flt.getLotaCadastranteSelId() != null
				&& flt.getLotaCadastranteSelId() != 0) {
			sbf.append(" and doc.lotaCadastrante.idLotacaoIni = :lotaCadastranteSelId");
 		}

		if (flt.getSubscritorSelId() != null && flt.getSubscritorSelId() != 0) {
			sbf.append(" and doc.subscritor.idPessoaIni = :subscritorSelId");
		}

		if (flt.getNmSubscritorExt() != null
				&& !flt.getNmSubscritorExt().trim().equals("")) {
			sbf.append(" and upper(doc.nmSubscritorExt) like :nmSubscritorExt");
		}

		if (flt.getOrgaoExternoSelId() != null
				&& flt.getOrgaoExternoSelId() != 0) {
			sbf.append(" and doc.orgaoExterno.idOrgao = :orgaoExternoSelId");
		}

		if (flt.getNumExtDoc() != null && !flt.getNumExtDoc().trim().equals("")) {
			sbf.append(" and upper(doc.numExtDoc) like :numExtDoc");
		}

		if (flt.getIdMod() != null && flt.getIdMod() != 0) {
			ExModelo mod = ExDao.getInstance().consultar(flt.getIdMod(),
					ExModelo.class, false);
			sbf.append(" and doc.exModelo.hisIdIni = :hisIdIni");
		}

		if (!apenasCount) {
			if (flt.getOrdem() == null || flt.getOrdem() == 0)
				sbf.append(" order by doc.dtDoc desc, doc.idDoc desc");
			else if (flt.getOrdem() == 1)
				sbf.append(" order by label.dtIniMarca desc, doc.idDoc desc");
			else if (flt.getOrdem() == 2)
				sbf.append(" order by doc.anoEmissao desc, doc.numExpediente desc, mob.numSequencia, doc.idDoc desc");
			else if (flt.getOrdem() == 3)
				sbf.append(" order by doc.dtFechamento desc, doc.idDoc desc");
			else if (flt.getOrdem() == 4)
				sbf.append(" order by doc.idDoc desc");
		}

		return sbf.toString();

	}

	public void setMontadorPrincipal(IMontadorQuery montadorQueryPrincipal) {
		// Este m�dodo n�o faz nada. � utilizado apenas para a extens�o da busca
		// textual do SIGA.
	}
}
