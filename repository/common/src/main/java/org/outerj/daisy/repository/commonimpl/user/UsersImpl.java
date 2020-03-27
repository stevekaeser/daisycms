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
package org.outerj.daisy.repository.commonimpl.user;

import java.util.ArrayList;

import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.Users;
import org.outerx.daisy.x10.UsersDocument;
import org.outerx.daisy.x10.UserDocument;

public class UsersImpl implements Users {
    private User[] users;
    public UsersImpl(User[] userArray) {
        users = userArray;
    }

    public User[] getArray() {
        return users;
    }

    public UsersDocument getXml() {
        UsersDocument usersDocument = UsersDocument.Factory.newInstance();
        UsersDocument.Users usersXml = usersDocument.addNewUsers();
        
        ArrayList usersList = new ArrayList();
        
        for (int i = 0; i < users.length; i++) {
            usersList.add(users[i].getXml().getUser());
        }
        
        usersXml.setUserArray((UserDocument.User[])usersList.toArray(new UserDocument.User[users.length]));
        return usersDocument;
    }

}
