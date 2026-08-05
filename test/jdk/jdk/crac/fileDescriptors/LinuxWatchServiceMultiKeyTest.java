/*
 * Copyright (c) 2026 Azul Systems, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import jdk.crac.management.CRaCMXBean;
import jdk.test.lib.Asserts;
import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracEngine;
import jdk.test.lib.crac.CracTest;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @summary Restore a WatchService that has more than one directory registered.
 * @library /test/lib
 * @build LinuxWatchServiceMultiKeyTest
 * @run driver jdk.test.lib.crac.CracTest
 * @requires (os.family == "linux")
 */
public class LinuxWatchServiceMultiKeyTest implements CracTest {

    private static final long TIMEOUT_SECONDS = 10;

    @Override
    public void test() throws Exception {
        new CracBuilder().engine(CracEngine.SIMULATE).doCheckpoint();
    }

    @Override
    public void exec() throws Exception {
        final int KEYS = 2;
        Path[] dirs = new Path[KEYS];
        WatchService watchService = FileSystems.getDefault().newWatchService();
        for (int i = 0; i < KEYS; i++) {
            dirs[i] = Files.createDirectory(Paths.get(System.getProperty("user.dir"), "workdir" + i));
            dirs[i].register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            verifyEvent(watchService, dirs[i]);
        }

        CRaCMXBean.getCRaCMXBean().checkpointRestore();

        for (Path dir : dirs) {
            verifyEvent(watchService, dir);
        }
        watchService.close();
    }

    private static void verifyEvent(WatchService watchService, Path dir) throws Exception {
        Files.createTempFile(dir, "temp", ".txt");
        WatchKey key = watchService.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Asserts.assertNotNull(key, "No ENTRY_CREATE event for " + dir);
        Asserts.assertEquals(key.watchable(), dir, "Event reported for an unexpected directory");
        Asserts.assertFalse(key.pollEvents().isEmpty(), "Watch key of " + dir + " has no events");
        key.reset();
    }
}
