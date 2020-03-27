/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.outerj.daisy.frontend.components.userregistrar;

import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.emailer.Emailer;
import org.outerj.daisy.util.Gpw;

import java.security.SecureRandom;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

public class UserRegistrarImpl implements UserRegistrar, Serviceable, Initializable, Configurable, ThreadSafe {
    private ServiceManager serviceManager;
    private String login;
    private String password;
    private String roles[];
    private String defaultRoleName;
    private Repository repository;
    private SecureRandom random = null;
    private int KEYLENGTH = 20;
    private String daisyHomePath;
    private Set<String> excludeUsers = new HashSet<String>();

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public void configure(Configuration configuration) throws ConfigurationException {
        Configuration registrarUser = configuration.getChild("registrarUser");
        login = registrarUser.getAttribute("login");
        password = registrarUser.getAttribute("password");

        Configuration[] roleConfs = configuration.getChild("roles").getChildren("role");
        roles = new String[roleConfs.length];
        for (int i = 0; i < roleConfs.length; i++) {
            roles[i] = roleConfs[i].getValue();
        }
        defaultRoleName = configuration.getChild("defaultRole").getValue(null);

        daisyHomePath = configuration.getChild("daisyHomePath").getValue(null);

        Configuration[] excludeUsersConf = configuration.getChild("excludeUsers").getChildren("login");
        for (Configuration anExcludeUsersConf : excludeUsersConf) {
            excludeUsers.add(anExcludeUsersConf.getValue());
        }
    }

    public void initialize() throws Exception {
        RepositoryManager repositoryManager = null;
        try {
            repositoryManager = (RepositoryManager)serviceManager.lookup("daisy-repository-manager");
            repository = repositoryManager.getRepository(new Credentials(login, password));
        } finally {
            if (repositoryManager != null)
                serviceManager.release(repositoryManager);
        }

        boolean hasAdminRole = false;
        long[] roles = repository.getAvailableRoles();
        for (long role : roles) {
            if (role == Role.ADMINISTRATOR) {
                hasAdminRole = true;
                break;
            }
        }
        if (!hasAdminRole)
            throw new Exception("The user to create user registrations, " + login + ", does not have the Administrator role.");
        

        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        }
        catch(java.security.NoSuchAlgorithmException nsae) {
            // maybe we are on IBM's SDK
            random = SecureRandom.getInstance("IBMSecureRandom");
        }
    }

    private Repository getRepository() {
        Repository repository = (Repository)((RemoteRepositoryImpl)this.repository).clone();
        repository.switchRole(Role.ADMINISTRATOR);
        return repository;
    }

    public void registerNewUser(String login, String password, String email, String firstName, String lastName,
            String server, String mountPoint, Locale locale) throws Exception {
        Repository repository = getRepository();
        UserManager userManager = repository.getUserManager();
        User user = userManager.createUser(login);
        user.setPassword(password);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUpdateableByUser(true);
        user.setConfirmed(false);
        String confirmKey = generateConfirmationKey();
        user.setConfirmKey(confirmKey);

        for (String roleName : roles) {
            Role role = userManager.getRole(roleName, false);
            user.addToRole(role);
        }
        if (defaultRoleName != null) {
            Role defaultRole = userManager.getRole(defaultRoleName, false);
            user.setDefaultRole(defaultRole);
        }
        user.save();

        ResourceBundle bundle = getBundle(locale);
        String message = bundle.getString("confirm-message-template");
        message = message.replaceAll("%%daisyHomePath%%", getDaisyHomePath(server, mountPoint));
        message = message.replaceAll("%%user%%", String.valueOf(user.getId()));
        message = message.replaceAll("%%confirmKey%%", confirmKey);

        Emailer emailer = (Emailer)repository.getExtension("Emailer");
        emailer.send(email, bundle.getString("confirm-message-subject"), message);
    }

    public void confirmUserRegistration(long userId, String confirmKey) throws Exception {
        Repository repository = getRepository();
        UserManager userManager = repository.getUserManager();
        User user = userManager.getUser(userId, true);

        if (user.isConfirmed()) {
            throw new RegistrarException("exception.already-confirmed", new Object[] {});
        }

        String referenceKey = user.getConfirmKey();
        if (referenceKey != null && !referenceKey.equals("") && referenceKey.equals(confirmKey)) {
            user.setConfirmed(true);
            user.setConfirmKey(null);
            user.save();
        } else {
            throw new RegistrarException("exception.failed-invalid-key", new Object[] {});
        }
    }

    private String generateConfirmationKey() {
        byte[] bytes = new byte[KEYLENGTH];
        char[] result = new char[KEYLENGTH * 2];

        random.nextBytes(bytes);

        for (int i = 0; i < KEYLENGTH; i++) {
            byte ch = bytes[i];
            result[2 * i] = Character.forDigit(Math.abs(ch >> 4), 16);
            result[2 * i + 1] = Character.forDigit(Math.abs(ch & 0x0f), 16);
        }

        return new String(result);
    }

    public void sendPasswordReminder(String login, String server, String mountPoint, Locale locale) throws Exception {
        if (excludeUsers.contains(login))
            throw new Exception("Requesting a new password for this user is not allowed.");

        Repository repository = getRepository();
        UserManager userManager = repository.getUserManager();
        User user = userManager.getUser(login, true);

        if (!user.getAuthenticationScheme().equals("daisy"))
            throw new RegistrarException("exception.not-daisy-scheme", new Object[0]);

        if (user.getEmail() == null || user.getEmail().equals(""))
            throw new RegistrarException("exception.no-email", new Object[] {login});

        String confirmKey = generateConfirmationKey();
        user.setConfirmKey(confirmKey);
        user.save();

        ResourceBundle bundle = getBundle(locale);
        String message = bundle.getString("password-reminder-template");
        message = message.replaceAll("%%login%%", login);
        message = message.replaceAll("%%user%%", String.valueOf(user.getId()));
        message = message.replaceAll("%%confirmKey%%", confirmKey);
        message = message.replaceAll("%%daisyHomePath%%", getDaisyHomePath(server, mountPoint));

        Emailer emailer = (Emailer)repository.getExtension("Emailer");
        emailer.send(user.getEmail(), bundle.getString("password-reminder-subject"), message);
    }

    public String assignNewPassword(long userId, String confirmKey) throws Exception {
        Repository repository = getRepository();
        UserManager userManager = repository.getUserManager();
        User user = userManager.getUser(userId, true);

        if (!user.getAuthenticationScheme().equals("daisy"))
            throw new RegistrarException("exception.not-daisy-scheme", new Object[0]);

        if (!confirmKey.equals(user.getConfirmKey())) {
            throw new RegistrarException("exception.pwd-failed-invalid-key", new Object[] {});
        }

        String password = Gpw.generate(8);
        user.setPassword(password);
        user.setConfirmKey(null);
        user.save();

        return password;
    }

    public void sendLoginsReminder(String email, String server, String mountPoint, Locale locale) throws Exception {
        Repository repository = getRepository();
        UserManager userManager = repository.getUserManager();
        User[] users = userManager.getUsersByEmail(email).getArray();

        if (users.length == 0)
            throw new RegistrarException("exception.no-user-with-that-email", new Object[] {email});

        StringBuilder logins = new StringBuilder(50);
        for (int i = 0; i < users.length; i++) {
            if (i > 0)
                logins.append(", ");
            logins.append(users[i].getLogin());
        }

        ResourceBundle bundle = getBundle(locale);
        String message = bundle.getString("logins-reminder-template");
        message = message.replaceAll("%%logins%%", logins.toString());
        message = message.replaceAll("%%daisyHomePath%%", getDaisyHomePath(server, mountPoint));

        Emailer emailer = (Emailer)repository.getExtension("Emailer");
        emailer.send(email, bundle.getString("logins-reminder-subject"), message);
    }

    private String getDaisyHomePath(String server, String mountPoint) {
        if (this.daisyHomePath != null)
            return this.daisyHomePath;
        else
            return server + mountPoint;
    }

    private ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("org/outerj/daisy/frontend/components/userregistrar/messages", locale);
    }
}
