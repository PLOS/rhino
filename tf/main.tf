provider "google" {
  project = var.project
  region  = "us-east1"
  version = "3.25"
}

provider "google-beta" {
  project = var.project
  region  = "us-east1"
  version = "3.25"
}

terraform {
  backend "gcs" {}
}

module "k8s-workload" {
  source    = "git@gitlab.com:plos/gcp-global.git//modules/k8s-workload"
  sa_name   = "rhino-sa"
  namespace = var.namespace
  cluster   = var.cluster
}

module "mysql" {
  source            = "git@gitlab.com:plos/gcp-global.git//modules/cloudsql"
  db_instance_name  = var.db_name
  db_schema_name    = "ambradb"
  availability_type = var.availability_type
  enable_public_ip  = true
  tier              = var.tier
}
