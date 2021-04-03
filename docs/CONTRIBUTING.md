# The Rules of Porting

1. <span id="mindiff">**"Min-Diff"**</span>  
   As little as possible code should be changed to maximize compatibility with future upstream commits. This includes file names, java names, and code formatting. There are a few exceptions to this rule:
    - **Import Order**  
      Because upstream's import order is currently inconsistent, it is better to make our import order consistent, even if it does not match with upstream's. Use the import order defined in [`checkstyle.xml`](../checkstyle/checkstyle.xml) or the editor config.
    - **Space Indentation**  
      If a **Java** file uses spaces for indentation, replace all space indentation with tab indentation. Upstream is also currently inconsistent with this, but for the most part uses tabs.  
      **Json** files should use *two spaces* for indentation.


2. **Replacing Code**  
   Directly replace code with alternatives from dependencies, such as vanilla, Fabric, or the library mod. If there is no current replacement, comment out the code by **highlighting and then pressing** `Ctrl+/` (`⌘+/`). **Do not use block comments or javadoc comments**.
   If the replacement needs mixins or is too large to directly replace (for example, doesn't fit in the same method), write the necessary code in the library mod instead of the main mod. Alternatively, a replacement could also be a dependency on another library mod.  
   Generally, the library mod is for code that does not depend on the main mod's code and does not have any "Create specific elements". This means that code that uses Create's name or ID, adds resources specifically for Create, or references a Create class should not go into the library mod. All new mixins and accessors should go into the library mod.  
   Some code, such as the Registrate datagen methods, have no current replacement and don't currently need one for the mod to function, but might have one in the future. For cases like these, do not delete the code and instead comment it out like specified before. The only difference now is that it is acceptable to use block comments if a whole line cannot be commented out (for example, a semicolon at the end of a builder pattern).


3. **Code Style**  
   Most code style rules are listed under [rule 1](#mindiff), so refer there for more information. Additionally, all new code that is added to the project must follow the checkstyle. This includes the library mod and replacement code in the main mod.


4. **Uncertainty**  
   If you are unsure of what to do with a certain section of code, discuss with the rest of the porting team before making changes.