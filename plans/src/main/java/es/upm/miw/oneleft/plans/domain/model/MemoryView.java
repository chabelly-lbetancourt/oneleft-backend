package es.upm.miw.oneleft.plans.domain.model;

import java.net.URI;

/**
 * A photo of an album as its group sees it.
 *
 * @param url a signed link to the photo, which expires
 */
public record MemoryView(Memory memory, URI url) {
}
