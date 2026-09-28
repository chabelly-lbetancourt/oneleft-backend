package es.upm.miw.oneleft.plans.domain.model;

/**
 * Actividades de los planes. Mismo catálogo que el servicio users; cada servicio tiene su propia copia para
 * no compartir código entre contextos (los códigos son el contrato).
 */
public enum Activity {
    PADEL, FUTBOL, BALONCESTO, TENIS, RUNNING, CICLISMO, SENDERISMO, JUEGOS_DE_MESA, CINE, CONCIERTOS
}
