package ch.puzzle.itc.mobiliar.business.property.control;

import ch.puzzle.itc.mobiliar.business.auditview.control.GenericAuditHandler;
import ch.puzzle.itc.mobiliar.business.auditview.entity.AuditViewEntry;
import ch.puzzle.itc.mobiliar.business.auditview.entity.AuditViewEntryContainer;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceContextEntity;
import ch.puzzle.itc.mobiliar.common.util.Tuple;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;

import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.math.BigDecimal;

@Stateless
@Named("propertyEntityAuditviewHandler")
public class PropertyEntityAuditviewHandler extends GenericAuditHandler {
    // Property on Master Resource
    private static final String SELECT_FOR_RESOURCE = "SELECT TAMW_RESOURCECONTEXT_ID " +
            "FROM TAMW_RESOURCECTX_PROP " +
            "WHERE PROPERTIES_ID = :propertyId";
    private static final String SELECT_FOR_RESOURCE_FROM_AUDIT = "SELECT TAMW_RESOURCECONTEXT_ID " +
            "FROM TAMW_RESOURCECTX_PROP_AUD " +
            "WHERE rev >= :rev " +
            "AND PROPERTIES_ID = :propertyId";
    private static final String SELECT_FOR_PROP_ON_RESOURCE = String.format("%s UNION %s", SELECT_FOR_RESOURCE, SELECT_FOR_RESOURCE_FROM_AUDIT);

    // Property on Consumed Resource
    private static final String SELECT_NAME_AND_CONTEXT_FOR_PROPERTY_ON_CONSUMED_RESOURCE =
            " SELECT " +
                    " consumed_resource.NAME || " +
                    "  CASE WHEN consumed_resource_relation.IDENTIFIER IS NOT NULL " +
                    "    THEN ' (' || consumed_resource_relation.IDENTIFIER || ')' " +
                    "    ELSE '' " +
                    "  END, " +
                    " resource_relation_context.CONTEXT_ID " +
                    " FROM TAMW_RESOURCE consumed_resource " +
                    " JOIN TAMW_CONSUMEDRESREL consumed_resource_relation " +
                    "     ON consumed_resource_relation.SLAVERESOURCE_ID = consumed_resource.ID " +
                    " JOIN TAMW_RESRELCONTEXT resource_relation_context " +
                    "     ON consumed_resource_relation.ID = resource_relation_context.CONSUMEDRESOURCERELATION_ID " +
                    " JOIN TAMW_RESRELCTX_PROP resource_relation_context_prop " +
                    "     ON resource_relation_context_prop.TAMW_RESRELCONTEXT_ID = resource_relation_context.ID " +
                    " WHERE resource_relation_context_prop.PROPERTIES_ID = :propertyId";

    private static final String SELECT_NAME_AND_CONTEXT_FOR_PROPERTY_ON_PROVIDED_RESOURCE =
            " SELECT provided_resource.NAME, resource_relation_context.CONTEXT_ID " +
                    " FROM TAMW_RESOURCE provided_resource " +
                    " JOIN TAMW_PROVIDEDRESREL provided_resource_relation " +
                    "     ON provided_resource_relation.SLAVERESOURCE_ID = provided_resource.ID " +
                    " JOIN TAMW_RESRELCONTEXT resource_relation_context " +
                    "     ON provided_resource_relation.ID = resource_relation_context.PROVIDEDRESOURCERELATION_ID " +
                    " JOIN TAMW_RESRELCTX_PROP resource_relation_context_prop " +
                    "     ON resource_relation_context_prop.TAMW_RESRELCONTEXT_ID = resource_relation_context.ID " +
                    " WHERE resource_relation_context_prop.PROPERTIES_ID = :propertyId";

//        String selectNameAndContextInAuditLog =
//                " SELECT provided_resource.NAME, resource_relation_context.CONTEXT_ID " +
//                        " FROM TAMW_RESOURCE_AUD provided_resource " +
//                        " JOIN TAMW_PROVIDEDRESREL_AUD provided_resource_relation " +
//                        "     ON provided_resource_relation.SLAVERESOURCE_ID = provided_resource.ID " +
//                        " JOIN TAMW_RESRELCONTEXT_AUD resource_relation_context " +
//                        "     ON provided_resource_relation.ID = resource_relation_context.PROVIDEDRESOURCERELATION_ID " +
//                        " JOIN TAMW_RESRELCTX_PROP_AUD resource_relation_context_prop " +
//                        "     ON resource_relation_context_prop.TAMW_RESRELCONTEXT_ID = resource_relation_context.ID " +
//                        " WHERE resource_relation_context_prop.PROPERTIES_ID = :propertyId " +
//                        " AND ROWNUM = 1" +
//                        " ORDER BY provided_resource_relation.REV DESC";

    @Override
    public AuditViewEntry createAuditViewEntry(AuditViewEntryContainer container) {
        ResourceContextEntity resourceContextEntity = getResourceContextEntityForPropertyOnMasterResource(container);
        boolean isPropertyOnMasterResource = resourceContextEntity != null;
        if (isPropertyOnMasterResource) {
            container.setEditContextId(resourceContextEntity.getId());
        } else {
            // property is on related resource (provided/consumed)
            Tuple<String, Integer> nameAndContext = getNameAndContextOfConsumedResource(container);
            boolean isPropertyOnConsumedResource = nameAndContext != null;
            if (isPropertyOnConsumedResource){
                container.setRelationName(String.format("%s: %s", AuditViewEntry.RELATION_CONSUMED_RESOURCE, nameAndContext.getA()));
                container.setEditContextId(nameAndContext.getB());
            } else {
                // property is on provided resource
                nameAndContext = getNameAndContextOfProvidedResource(container);
                container.setRelationName(String.format("%s: %s", AuditViewEntry.RELATION_PROVIDED_RESOURCE, nameAndContext.getA()));
                container.setEditContextId(nameAndContext.getB());
            }
        }
        return super.createAuditViewEntry(container);
    }

    private Tuple<String, Integer> getNameAndContextOfProvidedResource(AuditViewEntryContainer container) {
        try {
            Object[] nameAndId;
            nameAndId = (Object[]) entityManager
                    .createNativeQuery(SELECT_NAME_AND_CONTEXT_FOR_PROPERTY_ON_PROVIDED_RESOURCE)
                    .setParameter("propertyId", container.getEntityForRevision().getId())
                    .getSingleResult();
            String name = (String) nameAndId[0];
            int resourceContextId = ((BigDecimal) nameAndId[1]).intValue();
            return new Tuple<>(name, resourceContextId);
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     *
     * @param container
     * @return a Tuple&lt;Name of consumed Resource, ContextId&gt; or null
     */
    private Tuple<String, Integer> getNameAndContextOfConsumedResource(AuditViewEntryContainer container) {
        try {
            Object[] nameAndId;
            nameAndId = (Object[]) entityManager
                    .createNativeQuery(SELECT_NAME_AND_CONTEXT_FOR_PROPERTY_ON_CONSUMED_RESOURCE)
                    .setParameter("propertyId", container.getEntityForRevision().getId())
                    .getSingleResult();
            String name = (String) nameAndId[0];
            int resourceContextId = ((BigDecimal) nameAndId[1]).intValue();
            return new Tuple<>(name, resourceContextId);
        } catch (NoResultException e) {
            return null;
        }
    }

    public ResourceContextEntity getResourceContextEntityForPropertyOnMasterResource(AuditViewEntryContainer container) {
        try {
            Query query = entityManager
                    .createNativeQuery(SELECT_FOR_PROP_ON_RESOURCE)
                    .setParameter("rev", container.getRevEntity().getId())
                    .setParameter("propertyId", container.getEntityForRevision().getId());
            BigDecimal resourceContextId = (BigDecimal) query.getSingleResult();
            AuditReader reader = AuditReaderFactory.get(entityManager);
            return (ResourceContextEntity) reader.createQuery()
                    .forRevisionsOfEntity(ResourceContextEntity.class, true, true)
                    .add(AuditEntity.id().eq(resourceContextId.intValue()))
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
