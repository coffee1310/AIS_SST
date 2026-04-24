using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;

namespace Diplom_Stud
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : Application
    {
        public static string ApiBaseUrl = "http://185.246.66.164:8080/";

        public static string AuthToken { get; set; }

        public static UserData CurrentUser { get; set; }
    }

    public class UserData
    {
        public int Id { get; set; }
        public string Email { get; set; }
        public string Name { get; set; }
        public string Surname { get; set; }
        public List<string> Roles { get; set; }
        public string Token { get; set; }
        public string TokenType { get; set; }
    }
}