<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	buffer="64kb"%>
<%@ taglib tagdir="/WEB-INF/tags/mod" prefix="mod"%>
<%@ taglib uri="http://localhost/functiontag" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<table width="100%" border="0" bgcolor="#FFFFFF">
	<tr bgcolor="#FFFFFF">
		<td width="100%">
		<table width="100%">
			<tr>
				<td width="100%" align="left">
				<p style="font-family: AvantGarde Bk BT, Arial; font-size: 11pt;">PODER
				JUDICI�RIO</p>
				</td>
			</tr>
			<tr>
				<td width="100%" align="left">
				<p style="font-family: Arial; font-size: 10pt; font-weight: bold;">JUSTI�A
				FEDERAL</p>
				</td>
			</tr>
			<tr>
				<td width="100%" align="left">
				<p style="font-family: AvantGarde Bk BT, Arial; font-size: 8pt;"><c:choose>
					<c:when test="${empty mov}">${doc.lotaTitular.orgaoUsuario.descricaoMaiusculas}</c:when>
					<c:otherwise>${mov.lotaTitular.orgaoUsuario.descricaoMaiusculas}</c:otherwise>
				</c:choose><br />
				&nbsp;</p>
				</td>
			</tr>
		</table>
		</td>
	</tr>
</table>
