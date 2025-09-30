# Reflection Safety Rules for gomod123

## MANDATORY Reflection Safety Standards

### 1. Constructor Safety
- **NEVER** call constructors with null parameters unless explicitly documented as safe
- **ALWAYS** use proper constructor signatures with valid parameters
- For `ThreadDownloadImageData`: Use `new ThreadDownloadImageData(null, url, null, null)` or proper ImageBufferDownload implementation
- **NEVER** assume constructor parameters can be null without checking documentation

### 2. Field Access Safety
- **ALWAYS** wrap field access in try-catch blocks
- Use specific exception handling for `InvocationTargetException`
- Log reflection errors with context for debugging
- Never assume field names are stable across Minecraft versions
- **ALWAYS** check if field exists before accessing

### 3. Method Invocation Safety
- **ALWAYS** check for null before invoking methods via reflection
- Use proper exception handling for all reflection operations
- Provide fallback behavior when reflection fails
- **NEVER** assume method calls will succeed

### 4. Required Error Handling Pattern
```java
try {
    // Reflection operation
    java.lang.reflect.Field field = SomeClass.class.getDeclaredField("fieldName");
    field.setAccessible(true);
    Object value = field.get(target);
    // Use value...
} catch (java.lang.reflect.InvocationTargetException e) {
    System.err.println("[gomod] Reflection InvocationTargetException in [context]: " + e.getCause());
    // Continue with fallback behavior
} catch (java.lang.reflect.NoSuchFieldException e) {
    System.err.println("[gomod] Field not found: " + e.getMessage());
    // Continue with fallback behavior
} catch (Throwable e) {
    // Handle other reflection failures
    if (e instanceof java.lang.reflect.InvocationTargetException) {
        System.err.println("[gomod] Reflection InvocationTargetException: " + e.getCause());
    }
    System.err.println("[gomod] Reflection error in [context]: " + e.getMessage());
    // Continue with fallback behavior
}
```

### 5. Prohibited Patterns
- **NEVER** use reflection without proper error handling
- **NEVER** ignore reflection exceptions silently
- **NEVER** assume reflection operations will succeed
- **NEVER** use reflection for critical functionality without fallbacks
- **NEVER** use `catch (Exception ignored) {}` for reflection code
- **NEVER** call `setAccessible(true)` without checking if field exists

### 6. Required Documentation
- Document all reflection usage with purpose and fallback behavior
- Include version compatibility notes for field/method names
- Explain why reflection is necessary vs alternatives
- Document expected behavior when reflection fails

### 7. Testing Requirements
- Test reflection code with different Minecraft versions
- Test fallback behavior when reflection fails
- Verify error logging works correctly
- Test with different mod loaders (Weave, Forge, etc.)

### 8. Code Review Checklist
- [ ] All reflection operations wrapped in try-catch
- [ ] Specific handling for InvocationTargetException
- [ ] Proper error logging with context
- [ ] Fallback behavior implemented
- [ ] No silent exception swallowing
- [ ] Documentation explains reflection necessity
- [ ] Field/method names are version-safe

### 9. Common Reflection Anti-Patterns to Avoid
```java
// BAD - No error handling
Field field = SomeClass.class.getDeclaredField("fieldName");
field.setAccessible(true);
Object value = field.get(target);

// BAD - Silent exception swallowing
try {
    // reflection code
} catch (Exception ignored) {}

// BAD - Generic exception handling
try {
    // reflection code
} catch (Exception e) {
    // No specific handling for InvocationTargetException
}

// GOOD - Proper error handling
try {
    Field field = SomeClass.class.getDeclaredField("fieldName");
    field.setAccessible(true);
    Object value = field.get(target);
    // Use value...
} catch (java.lang.reflect.InvocationTargetException e) {
    System.err.println("[gomod] Reflection InvocationTargetException: " + e.getCause());
    // Fallback behavior
} catch (java.lang.reflect.NoSuchFieldException e) {
    System.err.println("[gomod] Field not found: " + e.getMessage());
    // Fallback behavior
} catch (Throwable e) {
    System.err.println("[gomod] Reflection error: " + e.getMessage());
    // Fallback behavior
}
```

### 10. Enforcement
- All reflection code must be reviewed against this checklist
- Any violation of these rules must be fixed before merge
- Regular audits of reflection usage in the codebase
- Update rules as new patterns are discovered

## Examples of Safe Reflection Usage

### Safe Field Access
```java
private boolean isTextFieldFocused(GuiScreen screen) {
    try {
        Field focusedField = screen.getClass().getDeclaredField("field_146209_f");
        focusedField.setAccessible(true);
        Object focused = focusedField.get(screen);
        return focused != null;
    } catch (java.lang.reflect.InvocationTargetException e) {
        System.err.println("[gomod] Reflection InvocationTargetException checking text field focus: " + e.getCause());
        return false; // Safe fallback
    } catch (java.lang.reflect.NoSuchFieldException e) {
        System.err.println("[gomod] Field 'field_146209_f' not found in " + screen.getClass().getSimpleName());
        return false; // Safe fallback
    } catch (Throwable e) {
        System.err.println("[gomod] Error checking text field focus: " + e.getMessage());
        return false; // Safe fallback
    }
}
```

### Safe Constructor Usage
```java
private void loadPlayerTexture(String uuid, String url) {
    try {
        ResourceLocation rl = new ResourceLocation("crafatar/avatars/" + uuid);
        TextureManager tm = Minecraft.getMinecraft().getTextureManager();
        ITextureObject tex = tm.getTexture(rl);
        if (tex == null) {
            // Safe constructor usage
            ThreadDownloadImageData data = new ThreadDownloadImageData(null, url, null, null);
            tm.loadTexture(rl, data);
        }
    } catch (Exception e) {
        System.err.println("[gomod] Error loading player texture: " + e.getMessage());
        // Fallback: use default texture or skip
    }
}
```

Remember: **Reflection is inherently fragile. Always assume it will fail and provide robust fallbacks.**
