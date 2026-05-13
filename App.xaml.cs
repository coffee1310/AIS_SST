using Diplom_Stud.Pages.Activist;
using System;
using System.IO;
using System.Text.Json;
using System.Windows;

namespace Diplom_Stud
{
    public partial class App : Application
    {
        public static string ApiBaseUrl = "http://185.246.66.164:8080/";

        public static bool IsActivistMode = false; 
        public static string AuthToken { get; set; }
        public static string RefreshToken { get; set; } 
        public static UserData CurrentUser { get; set; }
        public static UserProfileData CurrentUserProfile { get; set; }

        private static readonly string SessionFilePath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "Diplom_Stud", "session.json");

        public static void SaveSession(string refreshToken)
        {
            try
            {
                Directory.CreateDirectory(Path.GetDirectoryName(SessionFilePath));
                string json = JsonSerializer.Serialize(new { RefreshToken = refreshToken });
                File.WriteAllText(SessionFilePath, json);
            }
            catch { }
        }

        public static string LoadSession()
        {
            try
            {
                if (File.Exists(SessionFilePath))
                {
                    string json = File.ReadAllText(SessionFilePath);
                    using (JsonDocument doc = JsonDocument.Parse(json))
                    {
                        if (doc.RootElement.TryGetProperty("RefreshToken", out JsonElement tokenProp))
                        {
                            return tokenProp.GetString();
                        }
                    }
                }
            }
            catch { }
            return null;
        }

        public static void ClearSession()
        {
            try
            {
                if (File.Exists(SessionFilePath))
                {
                    File.Delete(SessionFilePath);
                }
            }
            catch { }
        }
    }

    public class UserData
    {
        public int Id { get; set; }
        public string Email { get; set; }
        public string Name { get; set; }
        public string Surname { get; set; }
        public System.Collections.Generic.List<string> Roles { get; set; }
        public string Token { get; set; }
        public string TokenType { get; set; }
    }
}