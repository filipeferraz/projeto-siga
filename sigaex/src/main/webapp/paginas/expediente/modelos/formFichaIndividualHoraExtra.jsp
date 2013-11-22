<%@ taglib tagdir="/WEB-INF/tags/mod" prefix="mod"%> 
<%@ taglib uri="http://localhost/sigatags" prefix="siga"%>
<%@ taglib uri="http://localhost/functiontag" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<mod:modelo>
	<mod:entrevista>
		<mod:grupo>
		    <mod:pessoa titulo="Matr�cula" var="servidor" />
		</mod:grupo>
		<mod:grupo> 
			<mod:lotacao titulo="Unidade Organizacional" var="unidade" />
		</mod:grupo>	
		<mod:grupo>
			<mod:selecao titulo="M�s" var="mes" 
			opcoes="Janeiro;Fevereiro;Mar�o;Abril;Maio;Junho;Julho;Agosto;Setembro;Outubro;Novembro;Dezembro"/> &nbsp;
			<mod:texto titulo="Ano" var="ano" maxcaracteres="4" largura="4"/> &nbsp;
			<mod:selecao titulo="N�mero de dias inclu�dos" 	var="numDiasHoraExtra"
			  opcoes="1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19;20;21;22;23;24;25;26;27;28;29;30;31"
			  reler="ajax" idAjax="diasAjax" />
		</mod:grupo>    		
		<mod:grupo depende="diasAjax">	
			<c:forEach var="i" begin="1" end="${numDiasHoraExtra}">
				<b>${i}� dia:</b>
				<mod:grupo>	
					<mod:selecao titulo="Dia do m�s"  var="diaMes${i}" 
			         	opcoes="1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19;20;21;22;23;24;25;26;27;28;29;30;31" />
			        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 						
					<mod:selecao titulo="Dia da semana" var="diaSemana${i}"
						opcoes="Domingo;Segunda-feira;Ter�a-feira;Quarta-feira;Quinta-feira;Sexta-feira;S�bado"/>
				</mod:grupo>
				<mod:grupo>
					<mod:hora titulo="In�cio" var="inicio1${i}" /> h  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;
					<mod:hora titulo="T�rmino" var="termino1${i}" /> h 
				</mod:grupo>
				<mod:grupo>
					<mod:hora titulo="In�cio" var="inicio2${i}" /> h  	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;
					<mod:hora titulo="T�rmino" var="termino2${i}" /> h 						
				</mod:grupo>	
				<mod:grupo>
					<mod:texto titulo="Total de Horas Extras no dia" var="totHrs${i}" largura="10"/>
				</mod:grupo>									
			</c:forEach>
		</mod:grupo>
		<br/>
		<mod:grupo>
			<mod:texto titulo="Total de Horas Extraordin�rias em Dias �teis e S�bados" var="totHrsDiasUteis" largura="10" />
		</mod:grupo>
		<mod:grupo>
			<mod:texto titulo="Total de Horas Extraordin�rias em Domingos e Feriados" var="totHrsDomingos" largura="10" />
		</mod:grupo>
		<mod:grupo>
			<mod:texto titulo="Total de Horas Extraordin�rias" var="totHrsFinal" largura="10"/>
		</mod:grupo>
		<br>
		<b><mod:mensagem texto="Obs: O subscritor da ficha � o pr�prio servidor. Ap�s a finaliza��o do documento, 
		 o superior hier�rquico e o titular da unidade <br> dever�o ser cadastrados como cossignat�rios exatamente
		  nessa ordem." vermelho="n�o">
		</mod:mensagem></b>	
	</mod:entrevista>

	<mod:documento>
		<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
		<head>
		<style type="text/css">
@page {
	margin-left: 3cm;
	margin-right: 2cm;
	margin-top: 1cm;
	margin-bottom: 2cm;
}
</style>
		</head>
		<body>
		<!-- INICIO PRIMEIRO CABECALHO
		<table width="100%" border="0" bgcolor="#FFFFFF"><tr><td>
		<c:import url="/paginas/expediente/modelos/inc_cabecalhoEsquerdaPrimeiraPagina.jsp" />
		</td></tr>
			<tr bgcolor="#FFFFFF">
				<td width="100%">
					<table width="100%">
						<tr><br /><br /><br /></tr>
						<tr>
							<td align="center"><font style="font-family:Arial;font-size:11pt;font-weight:bold;">
							FICHA INDIVIDUAL DE FREQUENCIA DE SERVI�O EXTRAORDIN�RIO</font></td>
						</tr>	
						<tr>
							<td align="center"><font style="font-family:Arial;font-size:11pt;">
							(Art.49 da Resolu��o n&ordm; 4/2008 - CJF)</font></td>
						</tr>											
					</table>
				</td>
			</tr>
		</table>
		FIM PRIMEIRO CABECALHO -->

		<!-- INICIO CABECALHO
		<c:import url="/paginas/expediente/modelos/inc_cabecalhoEsquerda.jsp" />
		FIM CABECALHO -->

		
		&nbsp;<br/> <%-- Solu��o by Edson --%>
		<b>Unidade Organizacional:</b> ${requestScope['unidade_lotacaoSel.sigla']}-${requestScope['unidade_lotacaoSel.descricao']}<br/>
		<b>M�s/Ano:</b>  ${mes}/${ano} <br/>
		<b>Nome:</b>  ${requestScope['servidor_pessoaSel.descricao']}<br/>
		<b>Matr�cula:</b>  ${requestScope['servidor_pessoaSel.sigla']} <br/> <br/>
		 

		<table width="100%" align="center" border="1" cellpadding="2"
			cellspacing="1" bgcolor="#000000">
			<tr>
				<td bgcolor="#999999" width="05%" align="center">Dia</td>
				<td bgcolor="#999999" width="15%" align="center">Dia da Semana</td>
				<td bgcolor="#999999" width="10%" align="center">In�cio</td>
				<td bgcolor="#999999" width="10%" align="center">T�rmino</td>
				<td bgcolor="#999999" width="10%" align="center">In�cio</td>
				<td bgcolor="#999999" width="10%" align="center">T�rmino</td>
				<td bgcolor="#999999" width="10%" align="center">Horas Extras</td>				
			
			</tr>
		</table>
		<c:forEach var="i" begin="1" end="${numDiasHoraExtra}">		    
    	    <table width="100%" align="center" border="1" cellpadding="2" cellspacing="1" bgcolor="#000000">
			<tr>
				<td bgcolor="#FFFFFF" width="05%" align="center">${requestScope[f:concat('diaMes',i)]}</td>
				<td bgcolor="#FFFFFF" width="15%" align="center">${requestScope[f:concat('diaSemana',i)]}</td>
			    <td bgcolor="#FFFFFF" width="10%" align="center">${requestScope[f:concat('inicio1',i)]} h</td>			    
				<td bgcolor="#FFFFFF" width="10%" align="center">${requestScope[f:concat('termino1',i)]} h</td>
				<td bgcolor="#FFFFFF" width="10%" align="center">${requestScope[f:concat('inicio2',i)]} h</td>			    
				<td bgcolor="#FFFFFF" width="10%" align="center">${requestScope[f:concat('termino2',i)]} h</td>
				<td bgcolor="#FFFFFF" width="10%" align="center">${requestScope[f:concat('totHrs',i)]} h</td>				
				 
			</tr>
		</table> 
		</c:forEach>
		<br/>
		<table width="100%" align="center" border="1" cellpadding="2" cellspacing="1" bgcolor="#000000">
			<tr>
				<td bgcolor="#FFFFFF" width="30%" align="left"><b>Total de Horas Extraordin�rias em Dias �teis e S�bados:</b> &nbsp; ${totHrsDiasUteis}</td>			
		    </tr>
		    <tr>
				<td bgcolor="#FFFFFF" width="30%" align="left"><b>Total de Horas Extraordin�rias em Domingos e Feriados:</b> &nbsp; ${totHrsDomingos}</td>		
		    </tr>
		    <tr>
				<td bgcolor="#FFFFFF" width="30%" align="left"><b>Total de Horas Extraordin�rias:</b> &nbsp; ${totHrsFinal}</td>		
		    </tr>		    
		</table>		
	    <br/> <br/> 
	    
	    <p style="text-align: center;">${doc.dtExtenso}</p> 
	    
		
	  
		<c:import url="/paginas/expediente/modelos/inc_assinatura.jsp" />
		



		<!-- INICIO PRIMEIRO RODAPE
		<c:import url="/paginas/expediente/modelos/inc_rodapeClassificacaoDocumental.jsp" />
		FIM PRIMEIRO RODAPE -->

		<!-- INICIO RODAPE
		<c:import url="/paginas/expediente/modelos/inc_rodapeNumeracaoADireita.jsp" />
		FIM RODAPE -->

		</body>
		</html>
	</mod:documento>
</mod:modelo>
