package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import life.genny.qwandaq.utils.ickle.expression.LiteralIckleExpression;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterContainer;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;

import java.io.Serializable;

public class LikeIcklePredicate extends AbstractSimpleIcklePredicate implements Serializable {
    private final IckleExpression<String> matchExpression;
    private final IckleExpression<String> pattern;
    private final IckleExpression<Character> escapeCharacter;

    public LikeIcklePredicate(
            IckleCriteriaBuilder criteriaBuilder,
            IckleExpression<String> matchExpression,
            IckleExpression<String> pattern) {
        this( criteriaBuilder, matchExpression, pattern, null );
    }

    public LikeIcklePredicate(
            IckleCriteriaBuilder criteriaBuilder,
            IckleExpression<String> matchExpression,
            String pattern) {
        this( criteriaBuilder, matchExpression, new LiteralIckleExpression<String>( criteriaBuilder, pattern) );
    }

    public LikeIcklePredicate(
            IckleCriteriaBuilder criteriaBuilder,
            IckleExpression<String> matchExpression,
            IckleExpression<String> pattern,
            IckleExpression<Character> escapeCharacter) {
        super( criteriaBuilder );
        this.matchExpression = matchExpression;
        this.pattern = pattern;
        this.escapeCharacter = escapeCharacter;
    }

    public LikeIcklePredicate(
            IckleCriteriaBuilder criteriaBuilder,
            IckleExpression<String> matchExpression,
            IckleExpression<String> pattern,
            char escapeCharacter) {
        this(
                criteriaBuilder,
                matchExpression,
                pattern,
                new LiteralIckleExpression<Character>( criteriaBuilder, escapeCharacter )
        );
    }

    public LikeIcklePredicate(
            IckleCriteriaBuilder criteriaBuilder,
            IckleExpression<String> matchExpression,
            String pattern,
            char escapeCharacter) {
        this(
                criteriaBuilder,
                matchExpression,
                new LiteralIckleExpression<String>( criteriaBuilder, pattern ),
                new LiteralIckleExpression<Character>( criteriaBuilder, escapeCharacter )
        );
    }

    public LikeIcklePredicate(
            IckleCriteriaBuilder criteriaBuilder,
            IckleExpression<String> matchExpression,
            String pattern,
            IckleExpression<Character> escapeCharacter) {
        this(
                criteriaBuilder,
                matchExpression,
                new LiteralIckleExpression<String>( criteriaBuilder, pattern ),
                escapeCharacter
        );
    }

    public IckleExpression<Character> getEscapeCharacter() {
        return escapeCharacter;
    }

    public IckleExpression<String> getMatchExpression() {
        return matchExpression;
    }

    public IckleExpression<String> getPattern() {
        return pattern;
    }

    public void registerParameters(IckleParameterRegistry registry) {
        IckleParameterContainer.Helper.possibleParameter( getEscapeCharacter(), registry );
        IckleParameterContainer.Helper.possibleParameter( getMatchExpression(), registry );
        IckleParameterContainer.Helper.possibleParameter( getPattern(), registry );
    }

    @Override
    public String render(boolean isNegated, IckleRenderingContext renderingContext) {
        final String operator = isNegated ? " not like " : " like ";
        StringBuilder buffer = new StringBuilder();
        buffer.append(getMatchExpression().render(renderingContext))
                .append(operator)
                .append(getPattern().render(renderingContext));
        if ( escapeCharacter != null ) {
            buffer.append(" escape ")
                    .append(getEscapeCharacter().render(renderingContext));
        }
        return buffer.toString();
    }
}
