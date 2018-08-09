/*
 * AMW - Automated Middleware allows you to manage the configurations of
 * your Java EE applications on an unlimited number of different environments
 * with various versions, including the automated deployment of those apps.
 * Copyright (C) 2013-2016 by Puzzle ITC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.puzzle.itc.mobiliar.business.security.control;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import ch.puzzle.itc.mobiliar.builders.ContextEntityBuilder;
import ch.puzzle.itc.mobiliar.builders.ResourceEntityBuilder;
import ch.puzzle.itc.mobiliar.builders.RestrictionDTOBuilder;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.integration.entity.util.ResourceTypeEntityBuilder;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.*;
import ch.puzzle.itc.mobiliar.business.security.entity.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;

import static org.mockito.Mockito.*;


@SuppressWarnings("static-access")
public class PermissionServiceTest {

	private static final String APP_DEVELOPER = "app_developer";
	private static final String CONFIG_ADMIN = "config_admin";
	private static final String VIEWER = "viewer";
	private static final String SERVER_ADMIN = "server_admin";
	private static final String SHAKEDOWN_ADMIN = "shakedown_admin";
	private static final String ROLE_NOT_DEPLOY = "role_not_deploy";
	
	private PermissionService permissionService;
	private SessionContext sessionContext;
	private PermissionRepository permissionRepository;
	private Map<String, List<RestrictionDTO>> myRoles;
    private ResourceEntityBuilder resourceEntityBuilder = new ResourceEntityBuilder();

	private ContextEntity global;
	private ContextEntity test;
	private ContextEntity envC;
	private ContextEntity envZ;
	private ResourceGroupEntity asResourceGroup;
	private ResourceGroupEntity appResourceGroup;

	private Principal principal;

    @Before
	public void setUp(){
		permissionService = new PermissionService();
		sessionContext = Mockito.mock(SessionContext.class);
		permissionService.sessionContext = sessionContext;
		permissionRepository = Mockito.mock(PermissionRepository.class);
		permissionService.permissionRepository = permissionRepository;
		// reset the static caches to avoid side effects
		permissionService.rolesWithRestrictions = null;
		permissionService.userRestrictions = null;

		global = new ContextEntityBuilder().id(1).buildContextEntity("GLOBAL", null, new HashSet<ContextEntity>(), false);
		test = new ContextEntityBuilder().id(5).buildContextEntity("TEST", global, new HashSet<ContextEntity>(), false);
		envC = new ContextEntityBuilder().id(10).buildContextEntity("C", test, new HashSet<ContextEntity>(), false);
		envZ = new ContextEntityBuilder().id(11).buildContextEntity("Z", test, new HashSet<ContextEntity>(), false);

		principal = new Principal() {
			@Override
			public String getName() {
				return "tester";
			}

		};

		asResourceGroup = new ResourceGroupEntity();
		asResourceGroup.setResourceType(ResourceTypeEntityBuilder.APPLICATION_SERVER_TYPE);

		appResourceGroup = new ResourceGroupEntity();
		appResourceGroup.setId(321);
		appResourceGroup.setResourceType(ResourceTypeEntityBuilder.APPLICATION_TYPE);
	}

	@Test
	public void shouldNotAllowToRemoveDefaultInstanceOfResTypeIfHasPermissionForResourcesOnly(){
		// given
		ResourceTypeEntity applicationResTypeEntity = new ResourceTypeEntity();
		applicationResTypeEntity.setName(DefaultResourceTypeDefinition.APPLICATION.name());

		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		PermissionEntity permission = new PermissionEntity();
		permission.setValue(Permission.RESOURCE.name());
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.DELETE);
		res.setResourceTypePermission(ResourceTypePermission.NON_DEFAULT_ONLY);
		res.setPermission(permission);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTO(res)));
		permissionService.rolesWithRestrictions = myRoles;
		// when
		boolean result = permissionService.hasPermissionToRemoveInstanceOfResType(applicationResTypeEntity);
		// then
		Assert.assertFalse(result);
	}
	
	@Test
	public void shouldNotAllowToRemoveInstanceOfNonDefaultResTypeIfHasPermissionToDeleteInstancesOfDefaultResourceTypeOnly(){
		// given
		ResourceTypeEntity nonDefaultResType = new ResourceTypeEntity();

		when(sessionContext.isCallerInRole(SERVER_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		PermissionEntity permission = new PermissionEntity();
		permission.setValue(Permission.RESOURCE.name());
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.DELETE);
		res.setResourceTypePermission(ResourceTypePermission.DEFAULT_ONLY);
		res.setPermission(permission);
		myRoles.put(SERVER_ADMIN, Arrays.asList(new RestrictionDTO(res)));
		permissionService.rolesWithRestrictions = myRoles;
		// when
		boolean result = permissionService.hasPermissionToRemoveInstanceOfResType(nonDefaultResType);
		// then
		Assert.assertFalse(result);
	}
	
	@Test
	public void shouldAllowToRemoveDefaultInstanceOfResTypeIfHasPermissionToDeleteInstancesOfDefaultResourceType(){
		// given
		ResourceTypeEntity applicationResTypeEntity = new ResourceTypeEntity();
		applicationResTypeEntity.setName(DefaultResourceTypeDefinition.APPLICATION.name());

		when(sessionContext.isCallerInRole(SERVER_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		PermissionEntity permission = new PermissionEntity();
		permission.setValue(Permission.RESOURCE.name());
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		res.setResourceTypePermission(ResourceTypePermission.DEFAULT_ONLY);
		res.setPermission(permission);
		myRoles.put(SERVER_ADMIN, Arrays.asList(new RestrictionDTO(res)));
		permissionService.rolesWithRestrictions = myRoles;
		// when
		boolean result = permissionService.hasPermissionToRemoveInstanceOfResType(applicationResTypeEntity);
		// then
		Assert.assertTrue(result);
	}

    @Test
	public void hasPermissionToDeleteResourceRelationWhenUserHasResourceUpdatePermissionAndResourceIsInstanceOfDefaultResourceType(){
		// given
        ResourceEntity app = resourceEntityBuilder.mockApplicationEntity("app", appResourceGroup, null);
		
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToDeleteRelation(app, new ContextEntity());

		// then
		Assert.assertTrue(result);
	}
	
	@Test
	public void hasPermissionToDeleteResourceRelationWhenUserHasResourceUpdatePermissionAndResourceIsInstanceOfNotDefaultResourceType(){
		// given
        ResourceEntity ws = resourceEntityBuilder.mockResourceEntity("ws", null, "webservice", null);

		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity upd = new RestrictionEntity();
		upd.setAction(Action.UPDATE);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, upd)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToDeleteRelation(ws, new ContextEntity());

		// then
		Assert.assertTrue(result);
	}

    @Test
    public void hasPermissionToDeleteResourceRelationWhenUserHasResourceUpdatePermissionAndResourceTypeIsNull() {
        // given
        ResourceEntity resourceWithoutResourceType = ResourceFactory.createNewResource("Orphan");

        when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        myRoles = new HashMap<>();
		RestrictionEntity upd = new RestrictionEntity();
		upd.setAction(Action.UPDATE);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, upd)));
        permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean result = permissionService.hasPermissionToDeleteRelation(resourceWithoutResourceType, new ContextEntity());

        // then
        Assert.assertFalse(result);
    }

	/**
	 * Screen AppServer: remove node relation
	 */
	@Test
	public void hasPermissionToDeleteResourceRelationWhenUserHasResourceUpdatePermissionAndResourceIsInstanceOfAppServerResType(){
		// given
        ResourceEntity as = resourceEntityBuilder.mockAppServerEntity("as", null, null, null);

		when(sessionContext.isCallerInRole(SERVER_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(SERVER_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToDeleteRelation(as, new ContextEntity());

		// then
		Assert.assertTrue(result);
	}
	
	@Test
	public void hasPermissionToDeleteResourceRelationWhenUserHasResourceUpdatePermissionAndResourceIsNotInstanceOfNodeResType(){
		// given
		ResourceEntity node = resourceEntityBuilder.mockNodeEntity("node", null,null);

		when(sessionContext.isCallerInRole(SERVER_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.UPDATE);
		myRoles.put(SERVER_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToDeleteRelation(node, new ContextEntity());

		// then
		Assert.assertTrue(result);
	}
	
	@Test
	public void hasPermissionToDeleteResourceRelationWhenUserIsAppDeveloperAndResourceIsInstanceOfApplicationResType(){
		// given
        ResourceEntity app = resourceEntityBuilder.mockApplicationEntity("app", appResourceGroup, null);

		when(sessionContext.isCallerInRole(APP_DEVELOPER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(APP_DEVELOPER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToDeleteRelation(app, new ContextEntity());

		// then
		Assert.assertTrue(result);
	}

	@Test
	public void hasPermissionToDeleteResourceRelationWhenUserHasPermissionToEditAllResourcesOnSpecificEnvironment(){
		// given
		ResourceEntity app = resourceEntityBuilder.mockApplicationEntity("app", appResourceGroup, null);

		when(sessionContext.isCallerInRole(APP_DEVELOPER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.UPDATE);
		res.setContext(envC);
		myRoles.put(APP_DEVELOPER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToDeleteRelation(app, envC);

		// then
		Assert.assertTrue(result);
	}

	@Test
	public void hasPermissionToDeleteResourceRelationWhenUserHasPermissionToEditSpecificResourceGroupOnAllEnvironments(){
		// given
		ResourceEntity app = resourceEntityBuilder.mockApplicationEntity("app", appResourceGroup, null);

		when(sessionContext.isCallerInRole(APP_DEVELOPER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.UPDATE);
		res.setResourceGroup(appResourceGroup);
		myRoles.put(APP_DEVELOPER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToDeleteRelation(app, envC) &&
				permissionService.hasPermissionToDeleteRelation(app, envZ);

		// then
		Assert.assertTrue(result);
	}

	@Test
	public void hasNoPermissionToDeleteResourceRelationWhenUserHasPermissionToEditAllResourcesOnAnotherEnvironment(){
		// given
		ResourceEntity app = resourceEntityBuilder.mockApplicationEntity("app", appResourceGroup, null);

		when(sessionContext.isCallerInRole(APP_DEVELOPER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.UPDATE);
		res.setContext(envC);
		myRoles.put(APP_DEVELOPER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToDeleteRelation(app, envZ);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void hasNoPermissionToDeleteResourceRelationWhenUserHasPermissionToEditSpecificResourceGroupOnAllEnvironments(){
		// given
		ResourceEntity app = resourceEntityBuilder.mockApplicationEntity("app", appResourceGroup, null);

		ResourceGroupEntity allowedApResourceGroup = new ResourceGroupEntity();
		allowedApResourceGroup.setId(4321);
		allowedApResourceGroup.setResourceType(new ResourceTypeEntityBuilder().id(1).parentResourceType(null).build());

		when(sessionContext.isCallerInRole(APP_DEVELOPER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		// Grant RESOURCE UPDATE on ResourceGroup 4321 to APP_DEVELOPER
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.UPDATE);
		res.setResourceGroup(allowedApResourceGroup);
		myRoles.put(APP_DEVELOPER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToDeleteRelation(app, envC);

		// then
		Assert.assertFalse(result);
	}
	
	@Test
	public void hasNoPermissionToDeleteResourceRelationWhenUserHasNoReourceUpdatePermissionAndResourceIsNotInstanceOfApplicationResType(){
		// given
        ResourceEntity as = resourceEntityBuilder.mockAppServerEntity("as", null, null, null);

		when(sessionContext.isCallerInRole(APP_DEVELOPER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(APP_DEVELOPER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCETYPE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToDeleteRelation(as, new ContextEntity());

		// then
		Assert.assertFalse(result);
	}

    @Test
    public void hasPermissionToDeleteResourceRelationWhenUserHasPermissionToUpdateResourceAndResourceIsRuntimeType(){
        // given
        ResourceEntity runtime = resourceEntityBuilder.mockRuntimeEntity("EAP6", null, null);

        when(sessionContext.isCallerInRole(SERVER_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(SERVER_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
        permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean result = permissionService.hasPermissionToDeleteRelation(runtime, new ContextEntity());

        // then
        Assert.assertTrue(result);
    }

    @Test
    public void hasPermissionToAddResourceRelationWhenUserHasPermissionToUpdateResource() {
        // given
        ResourceEntity as = resourceEntityBuilder.mockAppServerEntity("as", null, null, null);

        when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
        permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean canAdd = permissionService.hasPermissionToAddRelation(as, new ContextEntity());

        // then
        Assert.assertTrue(canAdd);
    }

    @Test
    public void hasNoPermissionToAddResourceRelationWhenResourceTypeIsNull() {
        // given
        //Create resource without resourceType
        ResourceEntity resourceWithoutResourceType = ResourceFactory.createNewResource("Orphan");
        //end Create resource without resourceType

        when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
        permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean canAdd = permissionService.hasPermissionToAddRelation(resourceWithoutResourceType, new ContextEntity());

        // then
        Assert.assertFalse(canAdd);
    }

    @Test
    public void hasPermissionToAddResourceRelationWhenUserHasResourceUpdatePermissionForApplicationServerResTypeAndResourceIsInstanceOfApplicationServerResType() {
        // given
        ResourceEntity as = resourceEntityBuilder.mockAppServerEntity("as", asResourceGroup, null, null);

        when(sessionContext.isCallerInRole(SERVER_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		// Grant RESOURCE UPDATE on APPLICATION_SERVER_TYPE to SERVER_ADMIN
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.UPDATE);
		res.setResourceType(ResourceTypeEntityBuilder.APPLICATION_SERVER_TYPE);
		myRoles.put(SERVER_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
        permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean canAdd = permissionService.hasPermissionToAddRelation(as, new ContextEntity());

        // then
        Assert.assertTrue(canAdd);
    }

    @Test
    public void hasNoPermissionToAddResourceRelationWhenUserUserHasResourceUpdatePermissionForApplicationServerResTypeAndResourceIsInstanceOfApplicationResType() {
        // given
        ResourceEntity app = resourceEntityBuilder.mockApplicationEntity("app", appResourceGroup, null);

        when(sessionContext.isCallerInRole(SERVER_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		res.setResourceType(ResourceTypeEntityBuilder.APPLICATION_SERVER_TYPE);
		myRoles.put(SERVER_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
        permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean canAdd = permissionService.hasPermissionToAddRelation(app, new ContextEntity());

        // then
        Assert.assertFalse(canAdd);
    }

    @Test
	public void hasPermissionToAddResourceRelationWhenUserHasPermissionToUpdateResourceAndResourceIsInstanceOfApplicationResType(){
		// given
        ResourceEntity app = resourceEntityBuilder.mockApplicationEntity("app", appResourceGroup, null);

		when(sessionContext.isCallerInRole(APP_DEVELOPER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity upd = new RestrictionEntity();
		upd.setAction(Action.UPDATE);
		myRoles.put(APP_DEVELOPER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, upd)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
        boolean canAdd = permissionService.hasPermissionToAddRelation(app, new ContextEntity());

		// then
		Assert.assertTrue(canAdd);
	}

    @Test
    public void hasNoPermissionToAddResourceRelationWhenUserUserHasNoPermissionToUpdateResourceAndResourceIsNotInstanceOfApplicationResType() {
        // given
        ResourceEntity as = resourceEntityBuilder.mockAppServerEntity("as", null, null, null);

        when(sessionContext.isCallerInRole(APP_DEVELOPER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.READ);
		myRoles.put(APP_DEVELOPER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
        permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean canAdd = permissionService.hasPermissionToAddRelation(as, new ContextEntity());

        // then
        Assert.assertFalse(canAdd);
    }

    @Test
    public void hasPermissionToTemplateModifyWhenUserIsShakedownAdminAndIsTestingMode() {
        // given
        ResourceEntity as = resourceEntityBuilder.mockAppServerEntity("as", null, null, null);

        when(sessionContext.isCallerInRole(SHAKEDOWN_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(SHAKEDOWN_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.SHAKEDOWN_TEST_MODE, res)));
		permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean result = permissionService.hasPermissionToAddResourceTemplate(as, true);

        // then
        Assert.assertTrue(result);
    }

    @Test
    public void hasPermissionToTemplateModifyWhenUserIsNotShakedownAdminAndIsTestingMode() {
        // given
        ResourceEntity as = resourceEntityBuilder.mockAppServerEntity("as", null, null, null);

        when(sessionContext.isCallerInRole(VIEWER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(VIEWER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.DEFAULT, res)));
        permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean result = permissionService.hasPermissionToAddResourceTemplate(as, true);

        // then
        Assert.assertFalse(result);
    }

    @Test
    public void hasNoPermissionToAddTemplateWhenUserHasPermissionToUpdateResourceAndIsResourceIsNotTestingMode() {
        // given
        ResourceEntity as = resourceEntityBuilder.mockAppServerEntity("as", null, null, null);

        when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
        permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean result = permissionService.hasPermissionToAddResourceTemplate(as, false);

        // then
        Assert.assertFalse(result);
    }

	@Test
	public void hasPermissionToAddTemplateWhenUserHasPermissionToCreateTemplateResourceAndIsResourceIsNotTestingMode() {
		// given
		ResourceEntity as = resourceEntityBuilder.mockAppServerEntity("as", null, null, null);

		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.CREATE);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE_TEMPLATE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToAddResourceTemplate(as, false);

		// then
		Assert.assertTrue(result);
	}

    @Test
    public void hasNoPermissionToAddResourceTypeTemplateWhenUserHasPermissionToUpdateResourceTypeAndIsApplicationResTypeAndIsNotTestingMode() {
        // given
		ResourceTypeEntity app = ResourceTypeEntityBuilder.APPLICATION_TYPE;

        when(sessionContext.isCallerInRole(APP_DEVELOPER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		res.setResourceTypePermission(ResourceTypePermission.ANY);
		myRoles.put(APP_DEVELOPER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCETYPE, res)));
        permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean result = permissionService.hasPermissionToAddResourceTypeTemplate(app, false);

        // then
        Assert.assertFalse(result);
    }

	@Test
	public void hasPermissionToAddResourceTypeTemplateWhenUserHasPermissionToCreateTemplateResourceTypeAndIsApplicationResTypeAndIsNotTestingMode() {
		// given
		ResourceTypeEntity app = ResourceTypeEntityBuilder.APPLICATION_TYPE;

		when(sessionContext.isCallerInRole(APP_DEVELOPER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.CREATE);
		res.setResourceTypePermission(ResourceTypePermission.ANY);
		myRoles.put(APP_DEVELOPER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCETYPE_TEMPLATE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToAddResourceTypeTemplate(app, false);

		// then
		Assert.assertTrue(result);
	}

    @Test
    public void hasPermissionToTemplateModifyWhenUserIsShakedownAdminAndIsNotTestingMode() {
        // given
        ResourceEntity as = resourceEntityBuilder.mockAppServerEntity("as", null, null, null);

        when(sessionContext.isCallerInRole(SHAKEDOWN_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(SHAKEDOWN_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.SHAKEDOWN_TEST_MODE, res)));
        permissionService.rolesWithRestrictions = myRoles;

        // when
        boolean result = permissionService.hasPermissionToAddResourceTemplate(as, false);

        // then
        Assert.assertFalse(result);
    }
	
	@Test
	public void hasPermissionToDeployWhenEmptyList(){
		// given
		List<RoleEntity>deployableRoles = new ArrayList<>();
		EntityManager entityManager = Mockito.mock(EntityManager.class);
		Query value = Mockito.mock(Query.class);
		when(entityManager.createQuery("from RoleEntity r where r.deployable=1")).thenReturn(value);
		when(value.getResultList()).thenReturn(deployableRoles);
		when(sessionContext.isCallerInRole(ROLE_NOT_DEPLOY)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		// when
		boolean result = permissionService.hasPermissionToSeeDeployment();
		// then
		Assert.assertFalse(result);
	}

	@Test
	public void multipleDeployUserRestriction(){
		// given
		List<RestrictionEntity> userRestriction = new LinkedList<>();

		ResourceGroupEntity resGroup1 = new ResourceGroupEntity();
		resGroup1.setId(111);
		resGroup1.setResourceType(new ResourceTypeEntityBuilder().id(1).parentResourceType(null).build());
		ResourceGroupEntity resGroup2 = new ResourceGroupEntity();
		resGroup2.setId(222);
		resGroup2.setResourceType(new ResourceTypeEntityBuilder().id(1).parentResourceType(null).build());
		ResourceGroupEntity forbiddenResGroup3 = new ResourceGroupEntity();
		forbiddenResGroup3.setId(333);
		forbiddenResGroup3.setResourceType(new ResourceTypeEntityBuilder().id(1).parentResourceType(null).build());

		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		res.setResourceGroup(resGroup1);
		PermissionEntity permission = new PermissionEntity();
		permission.setValue(Permission.DEPLOYMENT.name());
		res.setPermission(permission);
		userRestriction.add(res);
		RestrictionEntity res2 = new RestrictionEntity();
		res2.setAction(Action.ALL);
		res2.setResourceGroup(resGroup2);
		PermissionEntity permission2 = new PermissionEntity();
		permission2.setValue(Permission.DEPLOYMENT.name());
		res2.setPermission(permission2);
		userRestriction.add(res2);

		when(permissionRepository.getUserWithRestrictions(principal.getName())).thenReturn(userRestriction);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);

		// when
		boolean result1 = permissionService.hasPermission(Permission.DEPLOYMENT, envC, Action.CREATE, resGroup1, null);
		boolean result2 = permissionService.hasPermission(Permission.DEPLOYMENT, envC, Action.CREATE, resGroup2, null);
		boolean result3 = permissionService.hasPermission(Permission.DEPLOYMENT, envC, Action.CREATE, forbiddenResGroup3, null);

		// then
		Assert.assertTrue(result1);
		Assert.assertTrue(result2);
		Assert.assertFalse(result3);
	}
	
	@Test
	public void hasPermissionToAddResourceTypeTemplateWhenUserIsShakedownAdminAndIsTestingMode() {
		// given
		ResourceTypeEntity as = ResourceTypeEntityBuilder.APPLICATION_SERVER_TYPE;

		when(sessionContext.isCallerInRole(SHAKEDOWN_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(SHAKEDOWN_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.SHAKEDOWN_TEST_MODE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToAddResourceTypeTemplate(as, true);

		// then
		Assert.assertTrue(result);
	}

	@Test
	public void hasPermissionToAddResourceTypeTemplateWhenUserIsNotShakedownAdminAndIsTestingMode() {
		// given
		ResourceTypeEntity as = ResourceTypeEntityBuilder.APPLICATION_SERVER_TYPE;

		when(sessionContext.isCallerInRole(VIEWER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(VIEWER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.DEFAULT, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToAddResourceTypeTemplate(as, true);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void hasPermissionToAddResourceTypeTemplateWhenUserHasResourceTypePermissionAndIsNotTestingMode() {
		// given
		ResourceTypeEntity as = ResourceTypeEntityBuilder.APPLICATION_SERVER_TYPE;

		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		res.setResourceTypePermission(ResourceTypePermission.ANY);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCETYPE, res),
				new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCETYPE_TEMPLATE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToAddResourceTypeTemplate(as, false);

		// then
		Assert.assertTrue(result);
	}

	@Test
	public void hasNoPermissionToAddResourceTypeTemplateWhenUserHasOnlyResourceTypePermissionAndIsApplicationResTypeAndIsNotTestingMode() {
		// given
		ResourceTypeEntity app = ResourceTypeEntityBuilder.APPLICATION_TYPE;

		when(sessionContext.isCallerInRole(APP_DEVELOPER)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		res.setResourceTypePermission(ResourceTypePermission.ANY);
		myRoles.put(APP_DEVELOPER, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCETYPE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToAddResourceTypeTemplate(app, false);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void hasNoPermissionToAddResourceTypeTemplateWhenUserIsShakedownAdminAndIsNotTestingMode() {
		// given
		ResourceTypeEntity as = ResourceTypeEntityBuilder.APPLICATION_SERVER_TYPE;

		when(sessionContext.isCallerInRole(SHAKEDOWN_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(SHAKEDOWN_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.SHAKEDOWN_TEST_MODE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermissionToAddResourceTypeTemplate(as, false);

		// then
		Assert.assertFalse(result);
	}
	
	@Test
	public void permissionInTwoRoles(){
		// given
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(false);
		when(sessionContext.isCallerInRole(SERVER_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RELEASE, res)));
		myRoles.put(SERVER_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RELEASE, res)));
		permissionService.rolesWithRestrictions = myRoles;
			
		// when
		boolean result = permissionService.hasPermission(Permission.RELEASE);
		
		// then
		Assert.assertTrue(result);
	}
	
	@Test
	public void permissionInTwoRolesFail(){
		// given
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(false);
		when(sessionContext.isCallerInRole(SERVER_ADMIN)).thenReturn(false);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RELEASE, res)));
		myRoles.put(SERVER_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RELEASE, res)));
		permissionService.rolesWithRestrictions = myRoles;
			
		// when
		boolean result = permissionService.hasPermission(Permission.RELEASE);
		
		// then
		Assert.assertFalse(result);
	}

	@Test
	public void shouldOnlyReloadWhenNeeded() {
		// given
		PermissionService psMock = Mockito.mock(PermissionService.class);
		psMock.rolesWithRestrictions = new HashMap<>();

		// when
		psMock.getRolesWithRestrictions();

		// then
		verify(psMock, never()).reloadRolesWithRestrictionsList();
	}

	@Test
	public void shouldObtainRolesWithRestrictions() {
		// given
		when(permissionService.permissionRepository.getRolesWithRestrictions()).thenReturn(null);

		// when
		permissionService.getRolesWithRestrictions();

		// then
		verify(permissionService.permissionRepository, times(1)).getRolesWithRestrictions();
	}

	@Test
	public void shouldFailIfAPermissionCheckIsDoneWithoutContextAndPermissionIsGrantedToRoleOnSpecificContext() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
		resourceGroup.setId(23);
		resourceGroup.setResourceType(resourceType);
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.UPDATE);
		res.setContext(envC);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermission(Permission.RESOURCE, null, Action.UPDATE, resourceGroup, null);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void shouldFailIfAPermissionCheckIsDoneWithoutContextAndPermissionIsGrantedToUserOnSpecificContext() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
		resourceGroup.setId(23);
		resourceGroup.setResourceType(resourceType);
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		// Grant RESOURCE update on C
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.UPDATE);
		res.setContext(envC);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.RESOURCE.name());
		res.setPermission(perm);

		permissionService.rolesWithRestrictions = new HashMap<>();
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res));

		// when
		boolean result = permissionService.hasPermission(Permission.RESOURCE, null, Action.UPDATE, resourceGroup, null);

		// then
		Assert.assertFalse(result);
	}


	@Test
	public void shouldFailIfAPermissionIsRequiredOnAllContextsButOnlyGrantedToGroupOnASpecificContext() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
		resourceGroup.setId(23);
		resourceGroup.setResourceType(resourceType);
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		res.setContext(envC);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE_PROPERTY_DECRYPT, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermission(Permission.RESOURCE_PROPERTY_DECRYPT, null, Action.ALL, resourceGroup, null);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void shouldFailIfAPermissionIsRequiredOnAllContextsButOnlyGrantedToUserOnASpecificContext() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
		resourceGroup.setId(23);
		resourceGroup.setResourceType(resourceType);
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		res.setContext(envC);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.RESOURCE_PROPERTY_DECRYPT.name());
		res.setPermission(perm);
		myRoles = new HashMap<>();
		permissionService.rolesWithRestrictions = myRoles;
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res));

		// when
		boolean result = permissionService.hasPermission(Permission.RESOURCE_PROPERTY_DECRYPT, null, Action.ALL, resourceGroup, null);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void shouldSucceedIfAPermissionIsRequiredOnAllContextsAndGrantedToRoleOnAllContexts() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
		resourceGroup.setId(23);
		resourceGroup.setResourceType(resourceType);
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		myRoles = new HashMap<>();
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		myRoles.put(CONFIG_ADMIN, Arrays.asList(new RestrictionDTOBuilder().mockRestrictionDTO(Permission.RESOURCE_PROPERTY_DECRYPT, res)));
		permissionService.rolesWithRestrictions = myRoles;

		// when
		boolean result = permissionService.hasPermission(Permission.RESOURCE_PROPERTY_DECRYPT, null, Action.ALL, resourceGroup, null);

		// then
		Assert.assertTrue(result);
	}

	@Test
	public void shouldSucceedIfAPermissionIsRequiredOnAllContextsAndGrantedToUserOnAllContext() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
		resourceGroup.setId(23);
		resourceGroup.setResourceType(resourceType);
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.RESOURCE_PROPERTY_DECRYPT.name());
		res.setPermission(perm);
		myRoles = new HashMap<>();
		permissionService.rolesWithRestrictions = myRoles;
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res));

		// when
		boolean result = permissionService.hasPermission(Permission.RESOURCE_PROPERTY_DECRYPT, null, Action.ALL, resourceGroup, null);

		// then
		Assert.assertTrue(result);
	}

	@Test
	public void shouldReturnFalseIfCallerHasNoDelegationPermission() {
		// given
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.RESOURCE_PROPERTY_DECRYPT.name());
		res.setPermission(perm);
		myRoles = new HashMap<>();
		permissionService.rolesWithRestrictions = myRoles;
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res));

		// when
		boolean result = permissionService.hasPermissionToDelegatePermission(Permission.RESOURCE_PROPERTY_DECRYPT, null, null, null, Action.CREATE);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void shouldReturnFalseIfCallerHasDelegationPermissionButNotSimilarRestriction() {
		// given
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.PERMISSION_DELEGATION.name());
		res.setPermission(perm);
		myRoles = new HashMap<>();
		permissionService.rolesWithRestrictions = myRoles;
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res));

		// when
		boolean result = permissionService.hasPermissionToDelegatePermission(Permission.RESOURCE_PROPERTY_DECRYPT, null, null, null, Action.CREATE);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void shouldReturnFalseIfCallerHasDelegationPermissionButHisSimilarRestrictionIsRestrictedToAnExplicitResourceGroupAndTheOneHeWantsToDelegateIsNot() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
		resourceGroup.setId(23);
		resourceGroup.setResourceType(resourceType);
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.PERMISSION_DELEGATION.name());
		res.setPermission(perm);
		RestrictionEntity res2 = new RestrictionEntity();
		res2.setResourceGroup(resourceGroup);
		res2.setAction(Action.ALL);
		PermissionEntity perm2 = new PermissionEntity();
		perm2.setValue(Permission.RESOURCE_PROPERTY_DECRYPT.name());
		res2.setPermission(perm2);
		myRoles = new HashMap<>();
		permissionService.rolesWithRestrictions = myRoles;
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res, res2));

		// when
		boolean result = permissionService.hasPermissionToDelegatePermission(Permission.RESOURCE_PROPERTY_DECRYPT, null, null, null, Action.CREATE);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void shouldReturnFalseIfCallerHasDelegationPermissionButHisSimilarRestrictionIsRestrictedToAnExplicitResourceTypeAndTheOneHeWantsToDelegateIsFromAnother() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
		resourceGroup.setId(23);
		resourceGroup.setResourceType(resourceType);
		ResourceTypeEntity anotherResourceType = new ResourceTypeEntityBuilder().id(9).build();
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.PERMISSION_DELEGATION.name());
		res.setPermission(perm);
		RestrictionEntity res2 = new RestrictionEntity();
		res2.setResourceType(anotherResourceType);
		res2.setAction(Action.ALL);
		PermissionEntity perm2 = new PermissionEntity();
		perm2.setValue(Permission.RESOURCE_PROPERTY_DECRYPT.name());
		res2.setPermission(perm2);
		myRoles = new HashMap<>();
		permissionService.rolesWithRestrictions = myRoles;
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res, res2));

		// when
		boolean result = permissionService.hasPermissionToDelegatePermission(Permission.RESOURCE_PROPERTY_DECRYPT, resourceGroup, null, null, Action.CREATE);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void shouldReturnFalseIfCallerHasDelegationPermissionButHisSimilarRestrictionIsRestrictedToAnExplicitContextAndTheOneHeWantsToDelegateIsNot() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
		resourceGroup.setId(23);
		resourceGroup.setResourceType(resourceType);
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.PERMISSION_DELEGATION.name());
		res.setPermission(perm);
		RestrictionEntity res2 = new RestrictionEntity();
		res2.setResourceGroup(resourceGroup);
		res2.setAction(Action.ALL);
		res2.setContext(envC);
		PermissionEntity perm2 = new PermissionEntity();
		perm2.setValue(Permission.RESOURCE_PROPERTY_DECRYPT.name());
		res2.setPermission(perm2);
		myRoles = new HashMap<>();
		permissionService.rolesWithRestrictions = myRoles;
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res, res2));

		// when
		boolean result = permissionService.hasPermissionToDelegatePermission(Permission.RESOURCE_PROPERTY_DECRYPT, resourceGroup, null, null, Action.CREATE);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void shouldReturnFalseIfCallerHasDelegationPermissionButHisSimilarRestrictionIsRestrictedToAnExplicitResourceTypeAndTheOneHeWantsToDelegateIsNot() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		// Grant PERMISSION_DELEGATION
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.PERMISSION_DELEGATION.name());
		res.setPermission(perm);
		// Grant RESOURCE_PROPERTY_DECRYPT on resourceType
		RestrictionEntity res2 = new RestrictionEntity();
		res2.setResourceType(resourceType);
		res2.setAction(Action.ALL);
		res2.setContext(envC);
		PermissionEntity perm2 = new PermissionEntity();
		perm2.setValue(Permission.RESOURCE_PROPERTY_DECRYPT.name());
		res2.setPermission(perm2);
		permissionService.rolesWithRestrictions = new HashMap<>();
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res, res2));

		// when
		boolean result = permissionService.hasPermissionToDelegatePermission(Permission.RESOURCE_PROPERTY_DECRYPT, null, null, null, Action.CREATE);

		// then
		Assert.assertFalse(result);
	}

	@Test
	public void shouldReturnTrueIfCallerHasDelegationPermissionAndHisSimilarRestrictionIsRestrictedToAnExplicitResourceGroupAndAnExplicitContextWhichIsTheParentOfTheOneHeWantsToDelegate() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
		resourceGroup.setId(23);
		resourceGroup.setResourceType(resourceType);
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.PERMISSION_DELEGATION.name());
		res.setPermission(perm);
		RestrictionEntity res2 = new RestrictionEntity();
		res2.setResourceGroup(resourceGroup);
		res2.setAction(Action.ALL);
		res2.setContext(test);
		PermissionEntity perm2 = new PermissionEntity();
		perm2.setValue(Permission.RESOURCE_PROPERTY_DECRYPT.name());
		res2.setPermission(perm2);
		myRoles = new HashMap<>();
		permissionService.rolesWithRestrictions = myRoles;
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res, res2));

		// when
		boolean result = permissionService.hasPermissionToDelegatePermission(Permission.RESOURCE_PROPERTY_DECRYPT, resourceGroup, null, envC, Action.CREATE);

		// then
		Assert.assertTrue(result);
	}

	@Test
	public void shouldReturnTrueIfCallerHasDelegationPermissionAndHisSimilarRestrictionIsRestrictedToAnExplicitContextWhichIsTheParentOfTheOneHeWantsToDelegate() {
		// given
		ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().id(7).build();
		ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
		resourceGroup.setId(23);
		resourceGroup.setResourceType(resourceType);
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.PERMISSION_DELEGATION.name());
		res.setPermission(perm);
		RestrictionEntity res2 = new RestrictionEntity();
		res2.setAction(Action.ALL);
		res2.setContext(test);
		PermissionEntity perm2 = new PermissionEntity();
		perm2.setValue(Permission.RESOURCE_PROPERTY_DECRYPT.name());
		res2.setPermission(perm2);
		myRoles = new HashMap<>();
		permissionService.rolesWithRestrictions = myRoles;
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res, res2));

		// when
		boolean result = permissionService.hasPermissionToDelegatePermission(Permission.RESOURCE_PROPERTY_DECRYPT, null, null, envC, Action.CREATE);

		// then
		Assert.assertTrue(result);
	}

	@Test
	public void shouldReturnTrueIfCallerHasDelegationPermissionAndSimilarRestriction() {
		// given
		when(sessionContext.isCallerInRole(CONFIG_ADMIN)).thenReturn(true);
		when(sessionContext.getCallerPrincipal()).thenReturn(principal);
		// Grant PERMISSION_DELEGATION
		RestrictionEntity res = new RestrictionEntity();
		res.setAction(Action.ALL);
		PermissionEntity perm = new PermissionEntity();
		perm.setValue(Permission.PERMISSION_DELEGATION.name());
		res.setPermission(perm);
		// Grant RESOURCE_PROPERTY_DECRYPT for everything
		RestrictionEntity res2 = new RestrictionEntity();
		res2.setAction(Action.ALL);
		PermissionEntity perm2 = new PermissionEntity();
		perm2.setValue(Permission.RESOURCE_PROPERTY_DECRYPT.name());
		res2.setPermission(perm2);
		permissionService.rolesWithRestrictions = new HashMap<>();
		when(permissionRepository.getUserWithRestrictions(anyString())).thenReturn(Arrays.asList(res, res2));

		// when
		boolean result = permissionService.hasPermissionToDelegatePermission(Permission.RESOURCE_PROPERTY_DECRYPT, null, null, null, Action.CREATE);

		// then
		Assert.assertTrue(result);
	}
}