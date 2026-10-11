package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.Place;
import es.upm.miw.oneleft.plans.domain.model.PlaceQuery;

import java.util.List;

/**
 * Output port to a geocoder (HU-071). Empty when nothing matches or the geocoder does not answer: the organizer then
 * writes the place and uses their location, as before.
 */
public interface PlaceSearch {

    List<Place> search(PlaceQuery query);
}
