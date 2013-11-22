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
package br.gov.jfrj.siga.cp;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import br.gov.jfrj.siga.cp.model.HistoricoAuditavelSuporte;
import br.gov.jfrj.siga.dp.CpOrgaoUsuario;
import br.gov.jfrj.siga.dp.DpPessoa;
import br.gov.jfrj.siga.sinc.lib.Desconsiderar;

@MappedSuperclass
public abstract class AbstractCpIdentidade extends HistoricoAuditavelSuporte {
	@SequenceGenerator(name = "generator", sequenceName = "CP_IDENTIDADE_SEQ")
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "generator")
	@Column(name = "ID_IDENTIDADE", nullable = false)
	@Desconsiderar
	private Long idIdentidade;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_ORGAO_USU")
	private CpOrgaoUsuario cpOrgaoUsuario;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_TP_IDENTIDADE")
	private CpTipoIdentidade cpTipoIdentidade;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_PESSOA")
	private DpPessoa dpPessoa;
	@Column(name = "SENHA_IDENTIDADE")
	private String dscSenhaIdentidade;
	@Column(name = "SENHA_IDENTIDADE_CRIPTO")
	private String dscSenhaIdentidadeCripto;
	@Column(name = "SENHA_IDENTIDADE_CRIPTO_SINC")
	@Desconsiderar
	private String dscSenhaIdentidadeCriptoSinc;
	@Column(name = "DATA_CANCELAMENTO_IDENTIDADE")
	@Temporal(TemporalType.DATE)
	private Date dtCancelamentoIdentidade;
	@Column(name = "DATA_CRIACAO_IDENTIDADE")
	@Temporal(TemporalType.DATE)
	private Date dtCriacaoIdentidade;
	@Column(name = "DATA_EXPIRACAO_IDENTIDADE")
	@Temporal(TemporalType.DATE)
	private Date dtExpiracaoIdentidade;
	@Column(name = "LOGIN_IDENTIDADE")
	private String nmLoginIdentidade;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof AbstractCpIdentidade)) {
			return false;
		}
		AbstractCpIdentidade other = (AbstractCpIdentidade) obj;
		if (dscSenhaIdentidade == null) {
			if (other.dscSenhaIdentidade != null) {
				return false;
			}
		} else if (!dscSenhaIdentidade.equals(other.dscSenhaIdentidade)) {
			return false;
		}
		if (dtCancelamentoIdentidade == null) {
			if (other.dtCancelamentoIdentidade != null) {
				return false;
			}
		} else if (!dtCancelamentoIdentidade
				.equals(other.dtCancelamentoIdentidade)) {
			return false;
		}
		if (dtCriacaoIdentidade == null) {
			if (other.dtCriacaoIdentidade != null) {
				return false;
			}
		} else if (!dtCriacaoIdentidade.equals(other.dtCriacaoIdentidade)) {
			return false;
		}
		if (dtExpiracaoIdentidade == null) {
			if (other.dtExpiracaoIdentidade != null) {
				return false;
			}
		} else if (!dtExpiracaoIdentidade.equals(other.dtExpiracaoIdentidade)) {
			return false;
		}
		if (getHisDtFim() == null) {
			if (other.getHisDtFim() != null) {
				return false;
			}
		} else if (!getHisDtFim().equals(other.getHisDtFim())) {
			return false;
		}
		if (getHisDtIni() == null) {
			if (other.getHisDtIni() != null) {
				return false;
			}
		} else if (!getHisDtIni().equals(other.getHisDtIni())) {
			return false;
		}
		if (idIdentidade == null) {
			if (other.idIdentidade != null) {
				return false;
			}
		} else if (!idIdentidade.equals(other.idIdentidade)) {
			return false;
		}
		if (nmLoginIdentidade == null) {
			if (other.nmLoginIdentidade != null) {
				return false;
			}
		} else if (!nmLoginIdentidade.equals(other.nmLoginIdentidade)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the cpOrgaoUsuario
	 */
	public CpOrgaoUsuario getCpOrgaoUsuario() {
		return cpOrgaoUsuario;
	}

	/**
	 * @return the cpTipoIdentidade
	 */
	public CpTipoIdentidade getCpTipoIdentidade() {
		return cpTipoIdentidade;
	}

	/**
	 * @return the dpPessoa
	 */
	public DpPessoa getDpPessoa() {
		return dpPessoa;
	}

	/**
	 * @return the dscSenhaIdentidade
	 */
	public String getDscSenhaIdentidade() {
		return dscSenhaIdentidade;
	}

	public String getDscSenhaIdentidadeCripto() {
		return dscSenhaIdentidadeCripto;
	}

	public String getDscSenhaIdentidadeCriptoSinc() {
		return dscSenhaIdentidadeCriptoSinc;
	}

	/**
	 * @return the dtCancelamentoIdentidade
	 */
	public Date getDtCancelamentoIdentidade() {
		return dtCancelamentoIdentidade;
	}

	/**
	 * @return the dtCriacaoIdentidade
	 */
	public Date getDtCriacaoIdentidade() {
		return dtCriacaoIdentidade;
	}

	/**
	 * @return the dtExpiracaoIdentidade
	 */
	public Date getDtExpiracaoIdentidade() {
		return dtExpiracaoIdentidade;
	}

	/**
	 * @return the idCpIdentidade
	 */
	public Long getIdIdentidade() {
		return idIdentidade;
	}

	/**
	 * @return the nmLoginIdentidade
	 */
	public String getNmLoginIdentidade() {
		return nmLoginIdentidade;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((dscSenhaIdentidade == null) ? 0 : dscSenhaIdentidade
						.hashCode());
		result = prime
				* result
				+ ((dtCancelamentoIdentidade == null) ? 0
						: dtCancelamentoIdentidade.hashCode());
		result = prime
				* result
				+ ((dtCriacaoIdentidade == null) ? 0 : dtCriacaoIdentidade
						.hashCode());
		result = prime
				* result
				+ ((dtExpiracaoIdentidade == null) ? 0 : dtExpiracaoIdentidade
						.hashCode());
		result = prime * result
				+ ((getHisDtFim() == null) ? 0 : getHisDtFim().hashCode());
		result = prime * result
				+ ((getHisDtIni() == null) ? 0 : getHisDtIni().hashCode());
		result = prime * result
				+ ((idIdentidade == null) ? 0 : idIdentidade.hashCode());
		result = prime
				* result
				+ ((nmLoginIdentidade == null) ? 0 : nmLoginIdentidade
						.hashCode());
		return result;
	}

	/**
	 * @param cpOrgaoUsuario
	 *            the cpOrgaoUsuario to set
	 */
	public void setCpOrgaoUsuario(CpOrgaoUsuario cpOrgaoUsuario) {
		this.cpOrgaoUsuario = cpOrgaoUsuario;
	}

	/**
	 * @param cpTipoIdentidade
	 *            the cpTipoIdentidade to set
	 */
	public void setCpTipoIdentidade(CpTipoIdentidade cpTipoIdentidade) {
		this.cpTipoIdentidade = cpTipoIdentidade;
	}

	/**
	 * @param dpPessoa
	 *            the dpPessoa to set
	 */
	public void setDpPessoa(DpPessoa dpPessoa) {
		this.dpPessoa = dpPessoa;
	}

	/**
	 * @param dscSenhaIdentidade
	 *            the dscSenhaIdentidade to set
	 */
	public void setDscSenhaIdentidade(String dscSenhaIdentidade) {
		this.dscSenhaIdentidade = dscSenhaIdentidade;
	}

	public void setDscSenhaIdentidadeCripto(String senha) {
		this.dscSenhaIdentidadeCripto = senha;
	}

	public void setDscSenhaIdentidadeCriptoSinc(String senha) {
		this.dscSenhaIdentidadeCriptoSinc = senha;
	}

	/**
	 * @param dtCancelamentoIdentidade
	 *            the dtCancelamentoIdentidade to set
	 */
	public void setDtCancelamentoIdentidade(Date dtCancelamentoIdentidade) {
		this.dtCancelamentoIdentidade = dtCancelamentoIdentidade;
	}

	/**
	 * @param dtCriacaoIdentidade
	 *            the dtCriacaoIdentidade to set
	 */
	public void setDtCriacaoIdentidade(Date dtCriacaoIdentidade) {
		this.dtCriacaoIdentidade = dtCriacaoIdentidade;
	}

	/**
	 * @param dtExpiracaoIdentidade
	 *            the dtExpiracaoIdentidade to set
	 */
	public void setDtExpiracaoIdentidade(Date dtExpiracaoIdentidade) {
		this.dtExpiracaoIdentidade = dtExpiracaoIdentidade;
	}

	/**
	 * @param idCpIdentidade
	 *            the idCpIdentidade to set
	 */
	public void setIdIdentidade(Long idCpIdentidade) {
		this.idIdentidade = idCpIdentidade;
	}

	/**
	 * @param nmLoginIdentidade
	 *            the nmLoginIdentidade to set
	 */
	public void setNmLoginIdentidade(String nmLoginIdentidade) {
		this.nmLoginIdentidade = nmLoginIdentidade;
	}

}
