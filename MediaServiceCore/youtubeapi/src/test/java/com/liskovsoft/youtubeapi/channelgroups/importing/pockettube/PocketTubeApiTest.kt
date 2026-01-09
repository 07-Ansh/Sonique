package com.liskovsoft.youtubeapi.channelgroups.importing.pockettube

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import com.liskovsoft.sharedutils.TestHelpers
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class PocketTubeApiTest {
    @Before
    fun setUp() {
         
         
        System.setProperty("javax.net.ssl.trustStoreType", "JKS")
        ShadowLog.stream = System.out  
    }

    @Test
    fun testResult() {
        val pocketTubeContent = TestHelpers.readResource("channelgroups/pockettube.json")

         
        val groupNames: List<String> = JsonPath.read(pocketTubeContent, "$.ysc_collection.*~")  

        assertTrue("Group names not empty", groupNames.isNotEmpty())

        for (groupName in groupNames) {
             
            val channelIds: List<String> = JsonPath.read(pocketTubeContent, "$['$groupName']")

            assertTrue("Channel ids not empty", channelIds.isNotEmpty())
        }
    }

    @Test
    fun testWrongResult() {
        val pocketTubeContent = TestHelpers.readResource("channelgroups/pockettube.json")

         
        val groupNames: List<String> = try {
            JsonPath.read(pocketTubeContent, "$.ysc_collection.*~")
        } catch (e: PathNotFoundException) {
            return
        }

        assertTrue("Group names not empty", groupNames.isNotEmpty())

        for (groupName in groupNames) {
             
            val channelIds: List<String> = JsonPath.read(pocketTubeContent, "$['$groupName']")

            assertTrue("Channel ids not empty", channelIds.isNotEmpty())
        }
    }
}

