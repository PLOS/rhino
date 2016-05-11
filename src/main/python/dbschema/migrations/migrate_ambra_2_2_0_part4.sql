/* Create a trigger to add sort order to article assets - fix for PDEV-223 */
create trigger sortOrder_articleAsset before insert on articleAsset
for each row
begin
 declare recordCount int;
 select count(*) into recordCount from articleAsset where articleID = NEW.articleID;
 set NEW.sortOrder = recordCount;
end;