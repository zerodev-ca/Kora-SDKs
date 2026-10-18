package kora

import (
	"context"
	"encoding/json"
	"net/http"
	"net/url"
)

type Resource struct {
	client *Client
	name   string
	single string
}

func (r *Resource) List(ctx context.Context, query map[string]string) (*Page, error) {
	values := url.Values{}
	for key, value := range query {
		values.Set(key, value)
	}
	var data map[string]json.RawMessage
	if err := r.client.call(ctx, http.MethodGet, "/"+r.name+"?"+values.Encode(), nil, &data); err != nil {
		return nil, err
	}
	page := &Page{}
	if err := json.Unmarshal(data["total"], &page.Total); err != nil {
		return nil, err
	}
	if err := json.Unmarshal(data[r.name], &page.Rows); err != nil {
		return nil, err
	}
	return page, nil
}

func (r *Resource) Get(ctx context.Context, id string) (map[string]any, error) {
	var result map[string]any
	err := r.client.call(ctx, http.MethodGet, "/"+r.name+"/"+url.PathEscape(id), nil, &result)
	return result, err
}

func (r *Resource) one(ctx context.Context, method string, path string, fields map[string]any) (map[string]any, error) {
	var data map[string]json.RawMessage
	if err := r.client.call(ctx, method, path, fields, &data); err != nil {
		return nil, err
	}
	var result map[string]any
	err := json.Unmarshal(data[r.single], &result)
	return result, err
}

func (r *Resource) Create(ctx context.Context, fields map[string]any) (map[string]any, error) {
	return r.one(ctx, http.MethodPost, "/"+r.name, fields)
}

func (r *Resource) Update(ctx context.Context, id string, fields map[string]any) (map[string]any, error) {
	return r.one(ctx, http.MethodPatch, "/"+r.name+"/"+url.PathEscape(id), fields)
}

func (r *Resource) Delete(ctx context.Context, id string, force bool) error {
	path := "/" + r.name + "/" + url.PathEscape(id)
	if force {
		path += "?force=true"
	}
	return r.client.call(ctx, http.MethodDelete, path, nil, nil)
}
