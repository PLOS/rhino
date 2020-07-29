variable "project" {
  type        = string
  description = "name of google project (required)"
}

variable "namespace" {
  type        = string
  description = "k8s namespace (required)"
}

variable "db_name" {
  type        = string
  description = "name of db to create (required)"
}

variable "cluster" {
  type        = string
  description = "name of k8s cluster (required)"
}

variable "tier" {
  description = "The tier for the master instance."
  type        = string
  default     = "db-n1-standard-2"
}

variable "availability_type" {
  type        = string
  description = "REGIONAL|ZONAL"
  default     = "ZONAL"
}