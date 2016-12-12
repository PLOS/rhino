package org.ambraproject.rhino.model.ingest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.rest.RestClientException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ArticlePackage {

  private final ManifestXml manifest;
  private final ArticleItemInput articleItem;
  private final ImmutableList<ArticleItemInput> allItems;
  private final ImmutableList<ArticleFileInput> ancillaryFiles;

  ArticlePackage(ArticleItemInput articleItem, List<ArticleItemInput> assetItems,
                 List<ArticleFileInput> ancillaryFiles, ManifestXml manifest) {
    this.articleItem = Objects.requireNonNull(articleItem);
    this.allItems = ImmutableList.<ArticleItemInput>builder()
        .add(articleItem).addAll(assetItems).build();
    this.ancillaryFiles = ImmutableList.copyOf(ancillaryFiles);
    this.manifest = manifest;
  }

  public void validateAssetCompleteness(ArticleXml manuscript) {
    Set<Doi> manuscriptDois = manuscript.findAllAssetNodes().getDois();
    Set<Doi> packageDois = getAllItems().stream()
        .map(ArticleItemInput::getDoi)
        .collect(Collectors.toSet());
    Set<Doi> missingDois = Sets.difference(manuscriptDois, packageDois);
    if (!missingDois.isEmpty()) {
      String message = "Asset DOIs mentioned in manuscript are not included in package: "
          + missingDois.stream().map(Doi::getName).sorted().collect(Collectors.toList());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
  }

  public Doi getDoi() {
    return articleItem.getDoi();
  }

  public ImmutableList<ArticleItemInput> getAllItems() {
    return allItems;
  }

  public ImmutableList<ArticleFileInput> getAncillaryFiles() {
    return ancillaryFiles;
  }

  public ManifestXml getManifest() {
    return manifest;
  }
}
