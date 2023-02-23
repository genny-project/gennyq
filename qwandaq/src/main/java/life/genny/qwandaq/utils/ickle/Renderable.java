package life.genny.qwandaq.utils.ickle;

public interface Renderable {

    /**
     * Perform the rendering, returning the rendition
     */
    String render(IckleRenderingContext renderingContext);
}
