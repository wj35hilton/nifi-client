/*******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package nifi.client

import groovy.json.JsonSlurper
import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.ContentType.JSON

/**
 * Created by mburgess on 12/30/15.
 */
class Template implements Map<String, Object> {
    NiFi nifi
    private final JsonSlurper slurper = new JsonSlurper()
    protected Map<String, Object> propertyMap = [:]

    protected Template(NiFi nifi, Map<String, Object> propMap) {
        super()
        this.nifi = nifi
        this.propertyMap = propMap
    }

    @Override
    int size() {
        return propertyMap.size()
    }

    @Override
    boolean isEmpty() {
        return propertyMap.isEmpty()
    }

    @Override
    boolean containsKey(Object key) {
        return propertyMap.containsKey(key)
    }

    @Override
    boolean containsValue(Object value) {
        return propertyMap.containsValue(value)
    }

    @Override
    Object get(Object key) {
        return propertyMap.get(key)
    }

    @Override
    String put(String key, Object value) {
        throw new UnsupportedOperationException('Template property Map is immutable (for now)')
    }

    @Override
    String remove(Object key) {
        throw new UnsupportedOperationException('Template property Map is immutable (for now)')
    }

    @Override
    void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException('Template property Map is immutable (for now)')
    }

    @Override
    void clear() {
        throw new UnsupportedOperationException('Template property Map is immutable (for now)')
    }

    @Override
    Set<String> keySet() {
        return propertyMap.keySet()
    }

    @Override
    Collection<Object> values() {
        return propertyMap.values()
    }

    @Override
    Set<Map.Entry<String, Object>> entrySet() {
        return propertyMap.entrySet()
    }

    def instantiate() {
        def tId = this.id
        def n = this.nifi
        try {
            nifi.http.request(POST) {
                // TODO: other process groups besides root
                uri.path = '/nifi-api/process-groups/root/template-instance'

                requestContentType = JSON

                body = [
                  templateId: tId,
                  // TODO: ability to specify position
                  originX   : 200,
                  originY   : 300]

                response.success = { instanceResp ->
                // TODO: processors are nested ... which level should we be reloading here?
                    n.processors.reload()
                }

                response.'404' = { resp ->
                    throw new Exception("Error [${resp.statusLine}] instantiating template with ID $tID: ${resp.getData()}")
                }
            }
        } catch (e) {
            e.printStackTrace(System.err)
        }
    }

    def delete() {
        def deleteUri = this.template.uri
        nifi.http.request(DELETE) { uri.path=deleteUri }
        nifi.templates.reload()
    }

    def rightShift(file) {
        export(file)
    }

    def export(file) {
        def exportId = this.id
        def template = "${nifi.urlString}/nifi-api/controller/templates/$exportId".toURL().text.getBytes('UTF-8')
        (file as File).withOutputStream { out ->
            out.write(template)
        }
    }

}
