package es.upm.miw.oneleft.plans.domain.model;

/**
 * What the storage says about an uploaded photo.
 *
 * @param size        bytes
 * @param contentType as uploaded (the signed link only accepts the declared one)
 * @param content     what its first bytes say it really is; {@code null} if it is not a photo of these formats
 */
public record StoredMemory(long size, String contentType, MemoryFormat content) {

    /**
     * @throws ValidationException if what was uploaded is not a photo within the limits, or is not what it says
     */
    public StoredMemory requireValid() {
        var declared = MemoryFormat.of(contentType);
        MemorySize.require(size);
        if (content != declared) {
            throw new ValidationException("memory.notAPhoto", "The file is not the " + declared.contentType()
                    + " photo it says it is");
        }
        return this;
    }
}
