/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.services.jbpm.ui.form.render.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LayoutColumn {

    private String span;
    @JsonProperty("layoutComponents")
    private List<LayoutItem> items;
    
    private List<LayoutRow> rows;

    private String content;

    public String getSpan() {
        return span;
    }

    public void setSpan(String span) {
        this.span = span;
    }

    public List<LayoutItem> getItems() {
        return items;
    }

    public void setItems(List<LayoutItem> items) {
        this.items = items;
    }

    public List<LayoutRow> getRows() {
        return rows;
    }

    public void setRows(List<LayoutRow> rows) {
        this.rows = rows;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
    public void flatItems() {
        if (rows != null && !rows.isEmpty()) {
            for (LayoutRow row : rows) {
            this.items.addAll(collectItems(row));
            }
        }
    }

    protected List<LayoutItem> collectItems(LayoutRow row) {
        List<LayoutItem> collected = new ArrayList<>();
        for (LayoutColumn column : row.getColumns()) {
            collected.addAll(column.getItems());
            
            for (LayoutRow nestedRow : column.getRows()) {
                collected.addAll(collectItems(nestedRow));
            }
        }
        
        return collected;
    }
}
