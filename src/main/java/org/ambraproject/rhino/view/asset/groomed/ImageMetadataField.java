package org.ambraproject.rhino.view.asset.groomed;

import org.ambraproject.models.ArticleAsset;

/**
 * Fields that should be shown as a property of the entire figure and suppressed from the thumbnail metadata.
 */
enum ImageMetadataField {

  TITLE("title") {
    @Override
    protected Object access(ArticleAsset asset) {
      return asset.getTitle();
    }
  },
  DESCRIPTION("description") {
    @Override
    protected Object access(ArticleAsset asset) {
      return asset.getDescription();
    }
  },
  CONTEXT_ELEMENT("contextElement") {
    @Override
    protected Object access(ArticleAsset asset) {
      return asset.getContextElement();
    }
  };

  private final String memberName;

  private ImageMetadataField(String memberName) {
    this.memberName = memberName;
  }

  /**
   * @return the name to use for representing this field as a JSON object member
   */
  String getMemberName() {
    return memberName;
  }

  /**
   * @param asset an asset file object
   * @return the object's value for this field
   */
  abstract Object access(ArticleAsset asset);

}