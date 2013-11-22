package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import play.db.jpa.GenericModel;
import play.db.jpa.Model;

public enum SrFormaAcompanhamento {

	ANDAMENTO(1, "A cada andamento"), FECHAMENTO(2,
			"Quando o chamado for fechado"), NUNCA(3, "Não quero acompanhar");

	public long idFormaAcompanhamento;
	public String descrFormaAcompanhamento;

	SrFormaAcompanhamento(int id, String descricao) {
		this.idFormaAcompanhamento = id;
		this.descrFormaAcompanhamento = descricao;
	}

}
