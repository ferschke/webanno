/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * This class contains Utility methods that can be used in Project settings
 *
 *
 */
public class SecurityUtil
{
    private static final Log LOG = LogFactory.getLog(SecurityUtil.class);

    public static Set<String> getRoles(RepositoryService aProjectRepository, User aUser)
    {
        // When looking up roles for the user who is currently logged in, then we look in the
        // security context - otherwise we as the database.
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Set<String> roles = new HashSet<>();
        if (aUser.getUsername().equals(username)) {
            for (GrantedAuthority ga : SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities()) {
                roles.add(ga.getAuthority());
            }
        }
        else {
            for (Authority a : aProjectRepository.listAuthorities(aUser)) {
                roles.add(a.getAuthority());
            }
        }
        return roles;
    }
    
    /**
     * IS user super Admin
     * 
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user is a global admin.
     */
    public static boolean isSuperAdmin(RepositoryService aProjectRepository, User aUser)
    {
        boolean roleAdmin = false;
        for (String role : getRoles(aProjectRepository, aUser)) {
            if (Role.ROLE_ADMIN.name().equals(role)) {
                roleAdmin = true;
                break;
            }
        }
        return roleAdmin;
    }

    /**
     * IS project creator
     * 
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user is a project creator
     */
    public static boolean isProjectCreator(RepositoryService aProjectRepository, User aUser)
    {
        boolean roleAdmin = false;
        for (String role : getRoles(aProjectRepository, aUser)) {
            if (Role.ROLE_PROJECT_CREATOR.name().equals(role)) {
                roleAdmin = true;
                break;
            }
        }
        return roleAdmin;
    }

    /**
     * Determine if the User is allowed to update a project
     *
     * @param aProject the project
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user may update a project.
     */
    public static boolean isProjectAdmin(Project aProject, RepositoryService aProjectRepository,
            User aUser)
    {
        boolean projectAdmin = false;
        try {
            List<ProjectPermission> permissionLevels = aProjectRepository
                    .listProjectPermisionLevel(aUser, aProject);
            for (ProjectPermission permissionLevel : permissionLevels) {
                if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                        PermissionLevel.ADMIN.getName())) {
                    projectAdmin = true;
                    break;
                }
            }
        }
        catch (NoResultException ex) {
            LOG.info("No permision is given to this user " + ex);
        }

        return projectAdmin;
    }

    /**
     * Determine if the User is a curator or not
     *
     * @param aProject the project.
     * @param aProjectRepository the respository service.
     * @param aUser the user.
     * @return if the user is a curator.
     */
    public static boolean isCurator(Project aProject, RepositoryService aProjectRepository,
            User aUser)
    {
        boolean curator = false;
        try {
            List<ProjectPermission> permissionLevels = aProjectRepository
                    .listProjectPermisionLevel(aUser, aProject);
            for (ProjectPermission permissionLevel : permissionLevels) {
                if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                        PermissionLevel.CURATOR.getName())) {
                    curator = true;
                    break;
                }
            }
        }
        catch (NoResultException ex) {
            LOG.info("No permision is given to this user " + ex);
        }

        return curator;
    }

    /**
     * Determine if the User is member of a project
     *
     * @param aProject the project.
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user is a member.
     */
    public static boolean isMember(Project aProject, RepositoryService aProjectRepository,
            User aUser)
    {
        boolean user = false;
        try {
            List<ProjectPermission> permissionLevels = aProjectRepository
                    .listProjectPermisionLevel(aUser, aProject);
            for (ProjectPermission permissionLevel : permissionLevels) {
                if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                        PermissionLevel.USER.getName())) {
                    user = true;
                    break;
                }
            }
        }

        catch (NoResultException ex) {
            LOG.info("No permision is given to this user " + ex);
        }

        return user;
    }
    
    /**
     * Determine if the User is an admin of a project
     *
     * @param aProject the project.
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user is an admin.
     */
    public static boolean isAdmin(Project aProject, RepositoryService aProjectRepository,
            User aUser)
    {
        boolean user = false;
        try {
            List<ProjectPermission> permissionLevels = aProjectRepository
                    .listProjectPermisionLevel(aUser, aProject);
            for (ProjectPermission permissionLevel : permissionLevels) {
                if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                        PermissionLevel.ADMIN.getName())) {
                    user = true;
                    break;
                }
            }
        }

        catch (NoResultException ex) {
            LOG.info("No permision is given to this user " + ex);
        }

        return user;
    }
}
