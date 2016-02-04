create trigger sortOrder_articleRelationship before insert on articleRelationship
for each row
begin
 declare recordCount int;
 select count(*) into recordCount from articleRelationship where parentArticleID = NEW.parentArticleID;
 set NEW.sortOrder = recordCount;
end;